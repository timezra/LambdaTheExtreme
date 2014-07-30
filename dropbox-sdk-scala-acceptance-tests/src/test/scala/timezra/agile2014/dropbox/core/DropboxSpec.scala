package timezra.agile2014.dropbox.core

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers

@RunWith(classOf[JUnitRunner])
class DropboxSpec extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with Matchers {

  lazy val dropbox: Dropbox = Dropbox()

  feature("Dropbox accounts") {
    scenario("Gets account info") {
      Given("An existing user")

      When("She requests her account info")
      val accountInfo = dropbox accountInfo ()

      Then("She should receive it")
      accountInfo.uid should be > 0L
      accountInfo.display_name should not be (empty)
    }
  }
}