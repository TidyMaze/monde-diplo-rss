package controllers

import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.{Calendar, Date}

import com.gargoylesoftware.htmlunit.WebClient
import com.google.common.cache.CacheBuilder
import com.markatta.scalenium._
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import com.rometools.rome.feed.synd._
import com.rometools.rome.io.SyndFeedOutput

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}
import scalacache._
import scalacache.guava._
import scala.concurrent.duration._

import scalacache.modes.sync._

case class PublicationDate(year: Int, month: Int) {
  def getDatePath(): String = s"/$year/$month"
}

class CustomHtmlUnitDriver extends HtmlUnitDriver {
  override def modifyWebClient(client: WebClient): WebClient = {
    val modifiedClient = super.modifyWebClient(client)
    modifiedClient.getOptions.setCssEnabled(false)
    modifiedClient.getOptions.setJavaScriptEnabled(false)
    modifiedClient.getOptions.setThrowExceptionOnScriptError(false)
    modifiedClient
  }
}

case class ArticleLink(url: String, title: String)
case class Article(url: String, title: String, content: Seq[String], publishedAt: Instant)

object ArticleLink {
  implicit val fmt = Json.format[ArticleLink]
}

object Article {
  implicit val fmt = Json.format[Article]
}

object Application extends Controller {

  implicit val articlesCache: GuavaCache[Article] =
    GuavaCache(CacheBuilder.newBuilder().expireAfterAccess(java.time.Duration.ofNanos(7.days.toNanos)).build[String, Entry[Article]])

  val publicationDate = {
    val today = Calendar.getInstance()
    PublicationDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1)
  }

  val url = "https://www.monde-diplomatique.fr"
  def articlesUrl(publication: PublicationDate): String = s"$url${publicationDate.getDatePath()}/"

  def connexionUrl(): String = s"$url/connexion"

  def login(email: String, password: String)(implicit browser: Browser): Try[String] = {
    val connectionPage = goTo(connexionUrl())
    connectionPage.write(email).into("""input[name="email"]""")
    connectionPage.write(password).into("""input[name="mot_de_passe"]""")
    browser.first("""input[name="valider"]""").get.click()
    browser.first("div.statut_abo.argumentaire")
      .flatMap(e => Option(e.text)) match {
      case Some("Vous êtes abonné(e) à l’édition imprimée et aux archives du Monde diplomatique.") => Success(email)
      case _ => Failure(new RuntimeException(s"Couldn't do login for user `$email` with password `${password.map(_ => '*')}`"))
    }
  }

  def goTo(url: String)(implicit  browser: Browser): browser.type = {
    Logger.info(s"Go to $url")
    browser.goTo(url)
  }

  def fetchFeed()(implicit browser: Browser): Seq[ArticleLink] = goTo(articlesUrl(publicationDate))
    .find(s"""a[href^="${publicationDate.getDatePath()}"]""")
    .filter(el => el("href").matches(s"$url${publicationDate.getDatePath()}/[A-Za-z]+/\\d+"))
    .map(el => (Option(el("href")), el.find("h3").flatMap(v=>Option(v.text))))
    .collect {
      case (Some(link), texts) if texts.nonEmpty => ArticleLink(link, texts.mkString("\n"))
    }

  def fetchArticleContent(articleLink: ArticleLink)(implicit browser: Browser): Try[Article] =
    Try(goTo(articleLink.url)).map(page => {

      val regexYearMonth: Regex = """.*/(\d+)/(\d+)/""".r

      val (year, month) = page.first("""#entete > div.ariane > div.fil > a.filin""").map(_ ("href")).get match {
        case regexYearMonth(y, m) => (y, m)
      }

      Article(
        articleLink.url,
        articleLink.title,
        page.find("""div[class*="article-texte-"] p""").map(_.text),
        LocalDate.of(year.toInt, month.toInt, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
      )
    })

  def fetchArticleContentCached(articleLink: ArticleLink)(implicit browser: Browser): Try[Article] =
    articlesCache.get(articleLink.url) match {
      case Some(article) => {
        Logger.debug(s"Cache hit for ${articleLink.url}")
        Success(article)
      }
      case None =>
        Logger.debug(s"Cache miss for ${articleLink.url}")
        fetchArticleContent(articleLink)
          .map(article => {
            articlesCache.put(articleLink.url)(article)
            article
          })
    }


  def articleToEntry(syndFeed: SyndFeed)(article: Article): SyndEntry = {
    val entry = new SyndEntryImpl()
    entry.setUri(article.url)
    entry.setLink(article.url)
    entry.setSource(syndFeed)
    entry.setTitle(article.title)
    entry.setPublishedDate(Date.from(article.publishedAt))
    entry.setUpdatedDate(Date.from(article.publishedAt))

    val content = new SyndContentImpl()
    content.setType("text/plain")
    content.setValue(article.content.mkString("\n"))
    entry.setDescription(content)
    entry
  }

  def articlesToFeed(articles: Seq[Article]) = {
    val feed = new SyndFeedImpl
    feed.setFeedType("rss_2.0")
    feed.setTitle("Le Monde Diplomatique")
    feed.setDescription("Read full articles from Le Monde Diplomatique.")
    feed.setLink("https://monde-diplo-rss.herokuapp.com")
    feed.setEntries(articles.map(articleToEntry(feed)))
    new SyndFeedOutput().outputString(feed)
  }


  def getFeed(email: String, password: String) = Action {
    val driver = new CustomHtmlUnitDriver
    driver.setJavascriptEnabled(true)

    implicit val browser = new Browser(driver)

    val maybeOutput = login(email, password)
      .map(_ => fetchFeed())
      .map(allArticles =>
        allArticles.map(fetchArticleContentCached).foldLeft(Seq.empty[Article])({
          case (acc, Success(article)) => article +: acc
          case (acc, Failure(ex)) => {
            Logger.error("Article fetching error: ", ex)
            acc
          }
        })
      )
      .map(articlesToFeed)
    maybeOutput match {
      case Success(output) => Ok(output).as("application/rss+xml; charset=utf-8")
      case Failure(ex) => InternalServerError(ex.getMessage)
    }

  }
}

