//
//
// SNI Proxy
//
//
package Ale.Test
package io

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated }
import akka.util.ByteString
import akka.io.{ IO, Tcp }
import java.net.InetSocketAddress
import scala.util.Properties.{ lineSeparator => newLine }
import scala.io.Source

object TransProxyServiceApp {
  def main(args: Array[String]) {
    // initalize Global ActorSystem
    val system = ActorSystem("TransProxy-system")
    var activeListeners = false

    for (port <- args) {
      // start listening on each given socket
      if (port forall Character.isDigit) {

        if (port.toInt > 0) {
          val endpoint = new InetSocketAddress("0.0.0.0", port.toInt)
          system.actorOf(TransProxyService.props(endpoint), "tcp-proxy-service-" + port)
          activeListeners = true

        }
      }
    }

    if (activeListeners) readLine(s"Hit ENTER to exit ...$newLine")
    system.shutdown()

  }
}

object TransProxyService {

  def props(endpoint: InetSocketAddress): Props =
    Props(new TransProxyService(endpoint))
}

object Client {
  def props(target: InetSocketAddress, listenerActor: ActorRef) = {

    Props(new ClientTransProxyService(target, listenerActor))
  }
}

//service itself
class TransProxyService(endpoint: InetSocketAddress) extends Actor with ActorLogging {

  import context.system

  IO(Tcp) ! Tcp.Bind(self, endpoint)

  override def receive: Receive = {
    case Tcp.Connected(remote, _) =>
      log.debug("Remote address {} connected", remote)
      sender ! Tcp.Register(context.actorOf(TransProxyConnectionHandler.props(remote, sender)))
  }

}
//Client service itself
class ClientTransProxyService(remote: InetSocketAddress, listener: ActorRef) extends Actor with ActorLogging {
  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive = {
    case CommandFailed(_: Connect) ⇒
      println("Command failed")
      listener ! "failed"
      context stop self

    case c @ Connected(remote, local) ⇒
      listener ! "ready"
      println("Connected!")
      val connection = sender
      connection ! Register(self)
      context become {
        case data: ByteString ⇒
          connection ! Write(data); log.debug("sent data to client")
        case CommandFailed(w: Write) ⇒ log.debug("command failed")
        case Received(data)          ⇒ listener ! data; log.debug("data from endpoint received");
        case "close"                 ⇒ connection ! Close; log.debug("connection to endpoint closed");
        case _: ConnectionClosed     ⇒ context stop self; log.debug("connection closed");
      }
  }
}

object TransProxyConnectionHandler {

  def props(remote: InetSocketAddress, connection: ActorRef): Props =
    Props(new TransProxyConnectionHandler(remote, connection))
}

class TransProxyConnectionHandler(remote: InetSocketAddress, connection: ActorRef) extends Actor with ActorLogging {

  import Tcp._
  // We need to know when the connection dies without sending a `Tcp.ConnectionClosed`
  context.watch(connection)

  def parseSNI(data: List[Int]): String = {
    if (data.head != 0x16) {
      println("Not TLS :-(")
      return "none"
    }

    if (data(1) < 3 || (data(1) == 3 && data(2) < 1)) {
      println("SSL < 3.1 so it's still not TLS")
      return "none"
    }

    val restLength = data(3) + data(4)

    val rest = data.slice(5, (4 + restLength))

    var current = 0

    var handshakeType = rest(0)
    current += 1

    // Check Handshake
    if (handshakeType != 0x1) {
      println("Not a ClientHello")
    }

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

  def receive: Receive = {

    case Tcp.Received(data) => {
      val hostname = parseSNI(data.toList.map(x => (x & 0xFF)))
      hostname match {
        case "" => context.stop(self) // in case of
        case _ => {

          val client = context.actorOf(Client.props(new InetSocketAddress(hostname, 443), self));
          context become {
            case "ready" => {
              client ! data
              context become {
                case data: ByteString =>
                  connection ! Write(data); log.debug("Write to client: {}", remote)
                case Tcp.Received(data) =>
                  client ! data; log.debug("received data from client: {} ", remote)
                case _: Tcp.ConnectionClosed =>
                  context.stop(self); log.debug("Stopping, Connection with {} Closed", remote)
              }
            }
            case _ => context.stop(self)
          }
        }
      }
    }

    case _: Tcp.ConnectionClosed =>
      log.debug("Stopping, Connection with {} Closed", remote)
      context.stop(self)
  }
}

