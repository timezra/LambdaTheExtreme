package timezra.agile2014.dropbox.client

import com.typesafe.config.ConfigFactory
import timezra.agile2014.dropbox.core.Dropbox
import timezra.agile2014.dropbox.core.Implicits
import java.io.File

class Uploader(dropbox: Dropbox) {

  def putDirectory(root: File, path: String = "/"): Unit = {
    processDirectory(root, path)

    def processDirectory(dir: File, parentPath: String): Unit = {
      val path = s"${parentPath}/${dir.getName}"
      val files = dir.listFiles.map(f ⇒ f.getCanonicalFile())
      for (f ← files) {
        if (f.isDirectory) {
          processDirectory(f, path)
        } else {
          val filePath = s"${path}/${f.getName}"
          import Implicits._
          dropbox putFile (path = filePath, length = f.length().intValue(), contents = f)
        }
      }
    }
  }
}

object Uploader {
  val config = ConfigFactory.load().getConfig("timezra.dropbox.client").getConfig("test").getConfig("client")
  val clientIdentifier = config.getString("clientIdentifier")
  val accessToken = config.getString("accessToken")

  def apply(): Uploader = new Uploader(Dropbox(clientIdentifier, accessToken))

  def main(args: Array[String]) {
    Uploader().putDirectory(new File("src/test/resources/folder1"))
  }
}