/*
 * Copyright 2013 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package Ale.Test
package io

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated }
import akka.util.ByteString
import akka.io.{ IO, Tcp }
import java.net.InetSocketAddress
import scala.util.Properties.{ lineSeparator => newLine }

object TransProxyServiceApp extends App {
  val port = 11111
  val system = ActorSystem("TransProxy-system")

  val endpoint = new InetSocketAddress("localhost", port)
  system.actorOf(TransProxyService.props(endpoint), "tcp-proxy-service")
  readLine(s"Hit ENTER to exit ...$newLine")
  system.shutdown()
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
class ClientTransProxyService(remote: InetSocketAddress, listener: ActorRef) extends Actor {
  println("were in")
  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive = {
    case CommandFailed(_: Connect) ⇒
      println("WERE FAILED")
      listener ! "failed"
      context stop self

    case c @ Connected(remote, local) ⇒
      listener ! c
      val connection = sender
      connection ! Register(self)
      context become {
        case data: ByteString ⇒
          connection ! Write(data); println("WE SENT 1")
        case CommandFailed(w: Write) ⇒ println("WE SENT 2") // O/S buffer was full
        case Received(data) ⇒
          listener ! data; println(data); println("WE SENT3 ")
        case "close" ⇒
          connection ! Close; println("WE SENT4")
        case _: ConnectionClosed ⇒ context stop self; println("WE SENT5")
      }
  }
}

object TransProxyConnectionHandler {

  def props(remote: InetSocketAddress, connection: ActorRef): Props =
    Props(new TransProxyConnectionHandler(remote, connection))
}

class TransProxyConnectionHandler(remote: InetSocketAddress, connection: ActorRef) extends Actor with ActorLogging {

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
      if (hostname == "") context.stop(self);
      val client = context.actorOf(Client.props(new InetSocketAddress(hostname, 443), connection), "tcp-proxy-client");
      context become {
        case data: ByteString => connection ! data
        case Tcp.Received(data) =>
          println(data)
        case _: Tcp.ConnectionClosed =>
          log.debug("Stopping, Connection with {} Closed", remote)
          context.stop(self)
        case Terminated(`connection`) =>
          log.debug("Connection with {} Closed", remote)
          context.stop(self)

      }

      client ! "init"
      client ! data
    }

    case _: Tcp.ConnectionClosed =>
      log.debug("Stopping, Connection with {} Closed", remote)
      context.stop(self)
  }
}

