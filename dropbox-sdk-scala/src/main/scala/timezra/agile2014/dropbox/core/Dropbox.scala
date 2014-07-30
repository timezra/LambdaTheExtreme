package timezra.agile2014.dropbox.core

import spray.json.DefaultJsonProtocol
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.io.IO
import spray.can.Http
import akka.util.Timeout
import java.util.Locale
import scala.concurrent.Future
import spray.http.Uri

case class QuotaInfo(datastores: Int, shared: Long, quota: Long, normal: Long)
case class AccountInfo(referral_link: String, display_name: String, uid: Long, country: Option[String], quota_info: QuotaInfo, email: String)
object AccountInfoJsonProtocol extends DefaultJsonProtocol {
  implicit val quotaInfoFormat = jsonFormat4(QuotaInfo)
  implicit def accountInfoFormat = jsonFormat6(AccountInfo)
}

object Dropbox {
  def apply(clientIdentifier: String, accessToken: String): Dropbox = new Dropbox(clientIdentifier, accessToken)
}

class Dropbox(clientIdentifier: String, accessToken: String) {

  implicit lazy val system = ActorSystem("dropbox-sdk-scala")
  import system.dispatcher

  import spray.client.pipelining._
  def addUserAgent = addHeader("User-Agent", s"${clientIdentifier} Agile2014/1.0")
  def addAuthorization = addHeader("Authorization", s"Bearer ${accessToken}")

  import scala.concurrent.duration.DurationInt
  def accountInfo(conduit: ActorRef = IO(Http))(implicit timeout: Timeout = 60 seconds, locale: Option[Locale] = None): Future[AccountInfo] = {
    import AccountInfoJsonProtocol.accountInfoFormat
    import SprayJsonSupport.sprayJsonUnmarshaller

    val pipeline = (
      addUserAgent ~>
      addAuthorization ~>
      sendReceive(conduit) ~>
      unmarshal[AccountInfo]
    )
    val q = Seq(locale map ("locale" -> _.toLanguageTag)) flatMap (f ⇒ f)
    pipeline {
      Get(Uri("https://api.dropbox.com/1/account/info") withQuery (q: _*))
    }
  }
  
  def shutdown(): Unit = {
    import akka.pattern.ask
    import spray.util.pimpFuture

    IO(Http).ask(Http.CloseAll)(3 seconds).await
    system shutdown
  }
}

object ContentTypes {
  import spray.http.MediaType
  import spray.http.MediaTypes
  val `text/javascript` = MediaType custom ("text", "javascript", true, true)
  MediaTypes register `text/javascript`
}
