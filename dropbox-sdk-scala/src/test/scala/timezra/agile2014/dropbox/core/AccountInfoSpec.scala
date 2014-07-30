package timezra.agile2014.dropbox.core

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSpecLike
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.io.IO
import spray.can.Http
import spray.http.HttpRequest
import spray.http.HttpHeader
import spray.http.parser.HttpParser
import spray.http.HttpHeaders.RawHeader
import spray.http.HttpResponse
import spray.http.StatusCodes
import spray.http.HttpEntity
import spray.httpx.UnsuccessfulResponseException
import scala.concurrent.Future
import scala.concurrent.Await
import java.util.Locale

@RunWith(classOf[JUnitRunner])
class AccountInfoSpec(_system: ActorSystem) extends TestKit(_system) with FunSpecLike with Matchers with BeforeAndAfterAll {

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

  val Info = AccountInfo("https://db.tt/referralLink", "Display Name", 12345678, Some("KG"), QuotaInfo(0, 1, 2, 3), "test@email.com")
  val SuccessfulResponse = s"""
  {
      "referral_link": "${Info.referral_link}", 
      "display_name": "${Info.display_name}", 
      "uid": ${Info.uid}, 
      "country": "${Info.country.get}", 
      "quota_info": 
          {
              "datastores": ${Info.quota_info.datastores}, 
              "shared": ${Info.quota_info.shared}, 
              "quota": ${Info.quota_info.quota}, 
              "normal": ${Info.quota_info.normal}
          }, 
      "email": "${Info.email}"
  }
  """
      
  val AuthorizationFailure = s"""
    {"error": "The given OAuth 2 access token doesn't exist or has expired."}
  """

  override def afterAll {
    TestKit shutdownActorSystem system
  }

  describe("Account Info") {
    it("should make an http request") {
      val probe = ioProbe

      dropbox accountInfo probe.ref

      val expectedURI = "https://api.dropbox.com/1/account/info"
      probe expectMsg HttpRequest(uri = expectedURI, headers = List(authorizationHeader, userAgentHeader))
    }

    it("should parse account info") {
      val probe = ioProbe

      val response = dropbox accountInfo probe.ref

      probe expectMsgClass classOf[HttpRequest]
      probe reply (HttpResponse(entity = HttpEntity(ContentTypes.`text/javascript`, SuccessfulResponse)))

      await(response) should be(Info)
    }

    it("should propagate authorization failures") {
      val probe = ioProbe

      val response = dropbox accountInfo probe.ref

      probe expectMsgClass classOf[HttpRequest]
      probe reply (HttpResponse(status = StatusCodes.Unauthorized, entity = HttpEntity(ContentTypes.`text/javascript`, AuthorizationFailure)))

      intercept[UnsuccessfulResponseException] { await(response) }
    }

    it("should request language specific text") {
      val probe = ioProbe

      implicit val locale = Some(Locale.GERMANY)
      val response = dropbox accountInfo probe.ref

      val request = probe expectMsgClass classOf[HttpRequest]

      request.uri.query.get("locale") should be(locale.map(_.toLanguageTag))
    }

  }

  import scala.concurrent.duration.DurationInt
  def await[T](h: Future[T]): T = Await result (h, 1 second)
}