package timezra.agile2014.dropbox.core

import java.io.ByteArrayInputStream
import org.junit.runner.RunWith
import EnumeratorT.enumInputStream
import spray.http.ContentTypes.`application/octet-stream`
import spray.http.HttpEntity
import spray.http.HttpMethods
import spray.http.HttpProtocols.`HTTP/1.1`
import spray.http.HttpRequest
import spray.http.Uri
import spray.http.StatusCodes.Unauthorized
import spray.http.StatusCodes.LengthRequired
import org.scalatest.junit.JUnitRunner
import java.text.DateFormat
import java.text.SimpleDateFormat
import spray.http.HttpResponse
import ContentTypes.`text/javascript`
import java.util.Locale
import spray.httpx.UnsuccessfulResponseException
import akka.testkit.TestKit
import org.scalatest.FunSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import spray.http.HttpHeader
import spray.http.parser.HttpParser
import spray.http.HttpHeaders.RawHeader
import akka.testkit.TestProbe
import spray.can.Http
import akka.io.IO
import scala.concurrent.Future
import scala.concurrent.Await

@RunWith(classOf[JUnitRunner])
class PutFilesSpec(_system: ActorSystem) extends TestKit(_system) with FunSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("dropbox-sdk-scala-test"))

  val ClientIdentifier = "client_identifier"
  val AccessToken = "access_token"
  def header[V <% String](n: String, v: V): HttpHeader = HttpParser.parseHeader(RawHeader(n, v)).right.get
  val authorizationHeader = header("Authorization", s"Bearer $AccessToken")
  val userAgentHeader = header("User-Agent", s"$ClientIdentifier Agile2014/1.0")

  val dropbox = new Dropbox(ClientIdentifier, AccessToken)

  def ioProbe: TestProbe = {
    val probe = TestProbe()
    probe watch IO(Http)
    probe
  }

  val Root = "root"
  val Path = "test.txt"

  val Metadata = ContentMetadata("0 bytes", 0, "path", false, None, Some("rev"), None, false, "icon", Some(formatter.parse("Mon, 18 Jul 2011 20:13:43 +0000")), Some(formatter.parse("Wed, 20 Apr 2011 16:20:19 +0000")), "root", Some("text/plain"), Some(1), None)
  def formatter: DateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z")
  val SuccessfulResponse = s"""
  {
      "size": "${Metadata.size}",
      "bytes": ${Metadata.bytes},
      "path": "${Metadata.path}",
      "is_dir": ${Metadata.is_dir},
      "rev": "${Metadata.rev.get}",
      "thumb_exists": ${Metadata.thumb_exists},
      "icon": "${Metadata.icon}",
      "modified": "${formatter.format(Metadata.modified.get)}",
      "client_mtime": "${formatter.format(Metadata.client_mtime.get)}",
      "root": "${Metadata.root}",
      "mime_type": "${Metadata.mime_type.get}",
      "revision": ${Metadata.revision.get}
  }
  """
  val LengthRequiredFailure = s"""
    {"error": "The request did not specify the length of its content."}
  """

  import scalaz.effect.{ IO ⇒ zIO }
  describe("Files (PUT)") {
    it("should make an http request") {
      import EnumeratorT._

      val probe = ioProbe
      val contents = "ce n'est pas un test"
      val enum = enumInputStream[zIO](new ByteArrayInputStream(contents getBytes), 6)

      dropbox putFile (probe ref, Root, Path, enum, contents length)

      val request = probe expectMsgClass classOf[HttpRequest]

      request match {
        case HttpRequest(HttpMethods.PUT, uri, headers, HttpEntity.NonEmpty(`application/octet-stream`, data), `HTTP/1.1`) ⇒
          uri should be(Uri(s"https://api-content.dropbox.com/1/files_put/$Root/$Path"))
          headers should (contain(authorizationHeader) and contain(userAgentHeader))
          data.asString should be(contents)
      }
    }

    import Implicits._
    it("should set the content length header") {
      val probe = ioProbe
      val contents = "ce n'est pas un test"

      dropbox putFile (probe ref, path = Path, contents = contents, length = contents length)

      val request = probe expectMsgClass classOf[HttpRequest]

      request.headers should contain(header("Content-Length", String valueOf (contents length)))
    }

    it("should return content metadata") {
      val probe = ioProbe
      val contents = "ce n'est pas un test"

      val response = dropbox putFile (probe ref, path = Path, contents = contents, length = contents length)

      probe expectMsgClass classOf[HttpRequest]
      probe reply (HttpResponse(entity = HttpEntity(`text/javascript`, SuccessfulResponse)))

      await(response) should be(Metadata)
    }

    it("should specify a parent revision") {
      val probe = ioProbe
      val contents = "ce n'est pas un test"
      val parentRev = "1"

      val response = dropbox putFile (probe ref, path = Path, contents = contents, length = contents length, parent_rev = Some(parentRev))

      val request = probe expectMsgClass classOf[HttpRequest]

      request.uri.query.get("parent_rev").get should be(parentRev)
    }

    it("should specify whether to overwrite an existing file") {
      val probe = ioProbe
      val contents = "ce n'est pas un test"
      val overwrite = false

      val response = dropbox putFile (probe ref, path = Path, contents = contents, length = contents length, overwrite = Some(overwrite))

      val request = probe expectMsgClass classOf[HttpRequest]

      request.uri.query.get("overwrite").get.toBoolean should be(overwrite)
    }
  }
  
  import scala.concurrent.duration.DurationInt
  def await[T](h: Future[T]): T = Await result (h, 1 second)
}