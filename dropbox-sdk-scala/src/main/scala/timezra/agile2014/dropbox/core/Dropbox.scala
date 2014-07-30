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
import spray.json.RootJsonFormat
import spray.json.JsString
import java.util.Date
import spray.json.JsValue
import spray.json.DeserializationException
import java.text.DateFormat
import java.text.SimpleDateFormat
import spray.http.HttpData

case class QuotaInfo(datastores: Int, shared: Long, quota: Long, normal: Long)
case class AccountInfo(referral_link: String, display_name: String, uid: Long, country: Option[String], quota_info: QuotaInfo, email: String)
object AccountInfoJsonProtocol extends DefaultJsonProtocol {
  implicit val quotaInfoFormat = jsonFormat4(QuotaInfo)
  implicit def accountInfoFormat = jsonFormat6(AccountInfo)
}

object JsonImplicits {
  implicit object DateJsonFormat extends RootJsonFormat[Date] {
    def write(date: Date) = {
      JsString(formatter format date)
    }
    def read(value: JsValue) = value match {
      case null ⇒ null
      case JsString(date) ⇒ formatter parse date
      case _ ⇒ throw new DeserializationException("Date Expected with format %a, %d %b %Y %H:%M:%S %z")
    }
    def formatter: DateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z")
  }

  implicit object UriJsonFormat extends RootJsonFormat[Uri] {
    def write(uri: Uri) = JsString(uri.toString)
    def read(value: JsValue) = value match {
      case null ⇒ null
      case JsString(uri) ⇒ Uri(uri)
      case _ ⇒ throw new DeserializationException("Expected URI")
    }
  }
}

case class ContentMetadata(size: String, bytes: Long, path: String, is_dir: Boolean, is_deleted: Option[Boolean], rev: Option[String], hash: Option[String], thumb_exists: Boolean, icon: String, modified: Option[Date], client_mtime: Option[Date], root: String, mime_type: Option[String], revision: Option[Long], contents: Option[List[ContentMetadata]])
object ContentMetadataJsonProtocol extends DefaultJsonProtocol {
  import JsonImplicits._
  implicit def contentMetadataFormat: RootJsonFormat[ContentMetadata] = rootFormat(lazyFormat(jsonFormat15(ContentMetadata)))
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
      unmarshal[AccountInfo])
    val q = Seq(locale map ("locale" -> _.toLanguageTag)) flatMap (f ⇒ f)
    pipeline {
      Get(Uri("https://api.dropbox.com/1/account/info") withQuery (q: _*))
    }
  }

  import scalaz.effect.IoExceptionOr
  import scalaz.iteratee.EnumeratorT
  import scalaz.effect.{ IO ⇒ zIO }
  def putFile(conduit: ActorRef = IO(Http),
    root: String = "auto",
    path: String,
    contents: EnumeratorT[IoExceptionOr[(Array[Byte], Int)], zIO],
    length: Int,
    rev: Option[String] = None,
    parent_rev: Option[String] = None,
    overwrite: Option[Boolean] = None)(implicit timeout: Timeout = 15 minutes, locale: Option[Locale] = None): Future[ContentMetadata] = {
    import ContentMetadataJsonProtocol.contentMetadataFormat
    import SprayJsonSupport.sprayJsonUnmarshaller
    import MetaMarshallers._
    import Arrays._

    implicit def boundArray2HttpData(t: (Array[Byte], Int)): HttpData = HttpData(t._1 takeT t._2)

    val pipeline = (
      addUserAgent ~>
      addAuthorization ~>
      addHeader("Content-Length", String valueOf (length)) ~>
      sendReceive(conduit) ~>
      unmarshal[ContentMetadata])
    val q = Seq(parent_rev map ("parent_rev" ->), overwrite map ("overwrite" -> _.toString), locale map ("locale" -> _.toLanguageTag)) flatMap (f ⇒ f)
    pipeline {
      Put(Uri(s"https://api-content.dropbox.com/1/files_put/$root/$path") withQuery (q: _*), contents)
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
