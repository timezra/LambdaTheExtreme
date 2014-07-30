package timezra.agile2014.dropbox.core

case class AccountInfo(display_name: String, uid: Long)

object Dropbox {
  def apply(): Dropbox = new Dropbox()
}

class Dropbox() {

  def accountInfo(): AccountInfo = {
    AccountInfo(uid = 1, display_name = "Test Name")
  }
}
