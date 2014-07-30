package timezra.agile2014.dropbox.core

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSpecLike
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AccountInfoSpec extends FunSpecLike with Matchers with BeforeAndAfterAll {

  describe("Account Info") {
    it("should get account info") {
      new Dropbox().accountInfo should not be null
    }
  }
}