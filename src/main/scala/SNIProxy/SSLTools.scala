package SNIProxy

import scala.util.Try
import SNIHelpers.SSLExplorer
import java.nio.ByteBuffer
import scala.collection.convert.wrapAll._

object SSLTools {

  def sslNames(source: ByteBuffer) = SSLExplorer.explore(source).getServerNames.map(_.getEncoded().map(_.toChar).mkString).headOption

  def parseSNI(data: List[Int]): Try[String] = {
    Try {
      if (data.head != 0x16) throw new Exception("should not have come here")
      if (data(1) < 3 || (data(1) == 3 && data(2) < 1)) throw new Exception("should not have come here")
      else {
        val restLength = data(3) + data(4)

        val rest = data.slice(5, (4 + restLength))

        var current = 0

        var handshakeType = rest(0)
        current += 1

        // Check Handshake
        if (handshakeType != 0x1) throw new Exception("should not have come here")

        // Skip over another length
        current += 3
        // Skip over protocolversion
        current += 2
        // Skip over random number
        current += 4 + 28
        // Skip over session ID
        val sessionIDLength = rest(current)
        current += 1
        current += sessionIDLength

        val cipherSuiteLength = (rest(current) << 8) + rest(current + 1)
        current += 2
        current += cipherSuiteLength

        val compressionMethodLength = (rest(current))
        current += 1
        current += compressionMethodLength

        if (current > restLength) {
          println("no extensions")
        }

        var currentPos = 0

        // Skip over extensionsLength
        current += 2

        var hostname = ""
        while (current < restLength && hostname == "") {
          var extensionType = (rest(current) << 8) + rest(current + 1)
          current += 2

          var extensionDataLength = (rest(current) << 8) + rest(current + 1)
          current += 2

          if (extensionType == 0) {

            // Skip over number of names as we're assuming there's just one
            current += 2

            var nameType = rest(current)
            current += 1
            if (nameType != 0) {
              println("Not a hostname")
            }
            var nameLen = (rest(current) << 8) + rest(current + 1)
            current += 2

            hostname = rest.slice(current, current + nameLen).map(x => x.toChar).mkString
          }

          current += extensionDataLength
        }
        hostname
      }
    }
  }

}