package controllers

import java.util.Calendar

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import play.api.mvc._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import play.api.libs.json.Json

case class PublicationDate(year: Int, month: Int)

object Application extends Controller {

  val browser = JsoupBrowser()
  val publicationDate = {
    val today = Calendar.getInstance()
    PublicationDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1)
  }

  val url = "https://www.monde-diplomatique.fr"
  def articlesUrl(publication: PublicationDate): String = s"$url/${publicationDate.year}/${publicationDate.month}/"

  def index = Action {
    Ok(views.html.index(null))
  }

  def fetchFeed() = {
    val doc = browser.get(articlesUrl(publicationDate))
    val titles = doc >> elementList(s"""a[href^="/${publicationDate.year}/${publicationDate.month}"]""")
    val validatedTitles = titles.filter(el => el.attr("href").matches(s"/${publicationDate.year}/${publicationDate.month}/[A-Za-z]+/\\d+"))
    validatedTitles
      .map(el => (el.attr("href"), (el >?> element("h3") ).flatMap(_ >?> allText)))
      .collect {
        case (link, Some(_)) => url ++ link
      }
  }

  def getFeed() = Action {
    Ok(Json.prettyPrint(Json.toJson(fetchFeed())))
  }
}
