package forex.libs

import java.io.{ PrintWriter, StringWriter }

object StringUtils {

  implicit class StringOps(s: String) {
    def removeAllBlanks: String = s.replaceAll("\\s", "")

    def removeAllNotEnglishAlphabet: String = s.replaceAll("[^a-zA-Z]", "")
  }

  def printThrowable(t: Throwable): String = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw, true)
    t.printStackTrace(pw)
    sw.getBuffer.toString
  }
}
