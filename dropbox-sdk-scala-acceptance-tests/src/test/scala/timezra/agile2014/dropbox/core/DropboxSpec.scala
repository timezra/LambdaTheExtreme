package timezra.agile2014.dropbox.core

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import org.scalatest.FeatureSpecLike
import com.typesafe.config.ConfigFactory
import org.scalatest.ConfigMap
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@RunWith(classOf[JUnitRunner])
class DropboxSpec extends FeatureSpecLike with GivenWhenThen with BeforeAndAfterAll with Matchers {

  val config = ConfigFactory.load().getConfig("timezra.agile2014.dropbox").getConfig("test").getConfig("client")
  val clientIdentifier = config.getString("clientIdentifier")
  val accessToken = config.getString("accessToken")

  lazy val dropbox: Dropbox = Dropbox(clientIdentifier, accessToken)

  override def afterAll(configMap: ConfigMap) {
    super.afterAll(configMap)
    dropbox shutdown
  }

  feature("Dropbox accounts") {
    scenario("Gets account info") {
      Given("An existing user")

      When("She requests her account info")
      val accountInfo = Await result (dropbox accountInfo (), 5 seconds)

      Then("She should receive it")
      accountInfo.uid should be > 0L
      accountInfo.display_name should not be (empty)
      accountInfo.quota_info should not be (null)
    }
  }
}