//
//
// SNI Proxy
//
//
package SNIProxy

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated }
import akka.util.ByteString
import akka.io.{ IO, Tcp }
import java.net.InetSocketAddress
import scala.util.Properties.{ lineSeparator => newLine }
import scala.io.Source
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.http._
import HttpCharsets._
import MediaTypes._
import java.io.File

object SNIApp {
  import spray.httpx.SprayJsonSupport._
  import spray.json.DefaultJsonProtocol._
  import spray.util._

  sealed trait ClientProtocol
  object ClientConnectionReady extends ClientProtocol
  object ClientConnectionFailed extends ClientProtocol
  object ClientConnectionClosed extends ClientProtocol

  def startSNIService(port: Int, mappings: Map[String, String] = Map())(implicit as: ActorSystem) =
    as.actorOf(Props(new SNIService(new InetSocketAddress("0.0.0.0", port), mappings)), "SNIProxy-" + port)

  def extractMappings(mappingFile: File): Map[String, String] = {
    def unrollFile = if (mappingFile.canRead()) scala.io.Source.fromFile(mappingFile.getCanonicalPath).mkString else ""
    HttpEntity(
      contentType = ContentType(`application/json`, `UTF-8`),
      string = unrollFile).as[scala.collection.immutable.Map[String, String]] match {
        case Left(_)    => Map()
        case Right(map) => map
      }
  }

  def main(args: Array[String]) {

    implicit val system = ActorSystem()

    val sniServices = for (
      params <- CLIParser.parser.parse(args, Seq()).toSeq;
      instanceConfigurations <- params
    ) yield {
      {
        instanceConfigurations.mappings match {
          case Some(mappings) => startSNIService(instanceConfigurations.port, extractMappings(mappings))
          case None           => startSNIService(instanceConfigurations.port)
        }
      }

    }
    if (!sniServices.isEmpty) scala.io.StdIn.readLine(s"Hit ENTER to exit ...$newLine")
    system.shutdown()

  }

  //service itself
  class SNIService(endpoint: InetSocketAddress, mappings: Map[String, String]) extends Actor with ActorLogging {
    import context.system

    implicit val m = mappings
    implicit val p = endpoint.getPort

    IO(Tcp) ! Tcp.Bind(self, endpoint)

    override def receive: Receive = {
      case Tcp.Connected(remote, _) =>
        log.debug("Remote address {} connected", remote)
        sender ! Tcp.Register(context.actorOf(Props(new RequestHandler(remote, sender))))
    }

  }
  //Client service itself
  class SNIClient(remote: InetSocketAddress, listener: ActorRef) extends Actor with ActorLogging {
    import Tcp._
    import context.system

    IO(Tcp) ! Connect(remote)
    def receive = {
      case CommandFailed(_: Connect) ⇒
        listener ! ClientConnectionFailed
        context stop self
      case c @ Connected(remote, local) ⇒
        listener ! ClientConnectionReady
        val connection = sender
        connection ! Register(self)
        context become {
          case data: ByteString ⇒
            connection ! Write(data); log.debug("sent data to client")
          case CommandFailed(w: Write) ⇒ log.debug("command failed")
          case Received(data)          ⇒ listener ! data; log.debug("data from endpoint received");
          case Close                   ⇒ connection ! ClientConnectionClosed; log.debug("connection to endpoint closed");
          case _                       ⇒ context stop self; log.debug("connection closed");
        }
    }
  }

  class RequestHandler(remote: InetSocketAddress, connection: ActorRef)(implicit mappings: Map[String, String], listeningPort: Int) extends Actor with ActorLogging {
    import Tcp._

    context.watch(connection)

    def receive: Receive = {

      case Tcp.Received(data) =>
        SSLTools.sslNames(data.asByteBuffer) match {
          case None =>
            context.stop(self)
          case Some(hostname) => {
            val hostnameCleared = mappings.getOrElse(hostname, hostname)
            val client = context.actorOf(Props(new SNIClient(new InetSocketAddress(hostnameCleared, listeningPort), self)));
            context become {
              case ClientConnectionReady => {
                client ! data
                context become {
                  case data: ByteString =>
                    connection ! Write(data); log.debug("Write to client: {}", remote)
                  case Tcp.Received(data) =>
                    client ! data; log.debug("received data from client: {} ", remote)
                  case _: Tcp.ConnectionClosed =>
                    context.stop(self); log.debug("Stopping, Connection with {} Closed", remote)
                  case _ => context.stop(self); log.debug(" Connection Closed")
                }
              }
              case _ => context.stop(self)
            }
          }
        }

      case _: Tcp.ConnectionClosed =>
        log.debug("Stopping, Connection with {} Closed", remote)
        context.stop(self)
    }
  }
}
