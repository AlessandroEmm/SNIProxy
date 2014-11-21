package SNIProxy

import java.io.File

object CLIParser {

  case class InstanceConfiguration(port: Int, mappings: Option[File])

  val parser = new scopt.OptionParser[Seq[InstanceConfiguration]]("scopt") {
    head("scopt", "3.x")
    arg[(Int, File)]("portmap") optional () action {
      case ((port, file), ic) =>
        if (port < 65534) ic :+ InstanceConfiguration(port, Some(file))
        else ic
    } text ("List of Ports to listen on and a Mappingsfile")
    help("help") text ("prints this usage text")
  }
}
