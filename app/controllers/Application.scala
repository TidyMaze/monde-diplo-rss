package controllers

import java.util.Calendar

import com.gargoylesoftware.htmlunit.{ BrowserVersion, WebClient }
import play.api.mvc._
import play.api.libs.json.Json
import com.markatta.scalenium._
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import play.api.Logger

case class PublicationDate(year: Int, month: Int)

class CustomHtmlUnitDriver extends HtmlUnitDriver {
  override def modifyWebClient(client: WebClient): WebClient = {
    val modifiedClient = super.modifyWebClient(client)
    modifiedClient.getOptions.setThrowExceptionOnScriptError(false)
    modifiedClient
  }
}

case class ArticleLink(url: String, text: String)
object ArticleLink {
  implicit val fmt = Json.format[ArticleLink]
}

object Application extends Controller {

  private val driver = new CustomHtmlUnitDriver
  driver.setJavascriptEnabled(true)


  val publicationDate = {
    val today = Calendar.getInstance()
    PublicationDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1)
  }

  val url = "https://www.monde-diplomatique.fr"
  def articlesUrl(publication: PublicationDate): String = s"$url/${publicationDate.year}/${publicationDate.month}/"

  def index = Action {
    Ok(views.html.index(null))
  }

  def fetchFeed(browser: Browser) = {
    val doc: browser.type = {
      val destination = articlesUrl(publicationDate)
      println(s"Go to $destination")
      browser.goTo(destination)
    }
    val titles = doc.find(s"""a[href^="/${publicationDate.year}/${publicationDate.month}"]""")

    val validatedTitles = titles.filter(el =>
      el("href").matches(s"$url/${publicationDate.year}/${publicationDate.month}/[A-Za-z]+/\\d+")
    )

    validatedTitles
      .map(el => (
        Some(el("href")),
        el.find("h3").flatMap(v=>Option(v.text))
      ))
      .collect {
        case (Some(link), texts) if texts.nonEmpty => ArticleLink(link, texts.mkString("\n"))
      }
  }

  def getFeed() = Action {
    val browser = new Browser(driver)
    Ok(Json.prettyPrint(Json.toJson(fetchFeed(browser))))
  }
}
