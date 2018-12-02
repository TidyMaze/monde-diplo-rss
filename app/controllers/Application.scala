package controllers

import java.util.Calendar

import com.gargoylesoftware.htmlunit.WebClient
import com.markatta.scalenium._
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.util.{ Failure, Success, Try }

case class PublicationDate(year: Int, month: Int) {
  def getDatePath(): String = s"/$year/$month"
}

class CustomHtmlUnitDriver extends HtmlUnitDriver {
  override def modifyWebClient(client: WebClient): WebClient = {
    val modifiedClient = super.modifyWebClient(client)
    modifiedClient.getOptions.setThrowExceptionOnScriptError(false)
    modifiedClient
  }
}

case class ArticleLink(url: String, title: String)
case class Article(url: String, title: String, content: Seq[String])

object ArticleLink {
  implicit val fmt = Json.format[ArticleLink]
}

object Article {
  implicit val fmt = Json.format[Article]
}

object Application extends Controller {

  private val driver = new CustomHtmlUnitDriver
  driver.setJavascriptEnabled(true)


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

  def fetchFeed()(implicit browser: Browser) = goTo(articlesUrl(publicationDate))
    .find(s"""a[href^="${publicationDate.getDatePath()}"]""")
    .filter(el => el("href").matches(s"$url${publicationDate.getDatePath()}/[A-Za-z]+/\\d+"))
    .map(el => (Option(el("href")), el.find("h3").flatMap(v=>Option(v.text))))
    .collect {
      case (Some(link), texts) if texts.nonEmpty => ArticleLink(link, texts.mkString("\n"))
    }

  def fetchArticleContent(articleLink: ArticleLink)(implicit browser: Browser): Try[Article] =
    Try(goTo(articleLink.url)).map(page =>
      Article(articleLink.url, articleLink.title, page.find("""div[class*="article-texte-"] p""").map(_.text))
    )

  def getFeed(email: String, password: String) = Action {
    implicit val browser = new Browser(driver)

    val maybeOutput = login(email, password)
      .map(_ => fetchFeed())
      .map(allArticles =>
        allArticles.map(fetchArticleContent).foldLeft(Seq.empty[Article])({
          case (acc, Success(article)) => article +: acc
          case (acc, Failure(ex)) => {
            Logger.error("Article fetching error: ", ex)
            acc
          }
        })
      )
    maybeOutput match {
      case Success(output) => Ok(Json.prettyPrint(Json.toJson(output)))
      case Failure(ex) => InternalServerError(ex.getMessage)
    }

  }
}
