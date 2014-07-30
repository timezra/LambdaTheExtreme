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
  }
}