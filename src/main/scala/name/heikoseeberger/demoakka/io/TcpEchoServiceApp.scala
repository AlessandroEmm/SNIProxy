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

package name.heikoseeberger.demoakka
package io

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated }
import akka.io.{ IO, Tcp }
import java.net.InetSocketAddress
import scala.util.Properties.{ lineSeparator => newLine }

object TcpEchoServiceApp extends App {

  val system = ActorSystem("demo-system")
  val endpoint = new InetSocketAddress("localhost", 11111)
  system.actorOf(TcpEchoService.props(endpoint), "tcp-echo-service")

  readLine(s"Hit ENTER to exit ...$newLine")
  system.shutdown()
}

object TcpEchoService {

  def props(endpoint: InetSocketAddress): Props =
    Props(new TcpEchoService(endpoint))
}

class TcpEchoService(endpoint: InetSocketAddress) extends Actor with ActorLogging {

  import context.system

  IO(Tcp) ! Tcp.Bind(self, endpoint)

  override def receive: Receive = {
    case Tcp.Connected(remote, _) =>
      log.debug("Remote address {} connected", remote)
      sender ! Tcp.Register(context.actorOf(TcpEchoConnectionHandler.props(remote, sender)))
  }
}

object TcpEchoConnectionHandler {

  def props(remote: InetSocketAddress, connection: ActorRef): Props =
    Props(new TcpEchoConnectionHandler(remote, connection))
}

class TcpEchoConnectionHandler(remote: InetSocketAddress, connection: ActorRef) extends Actor with ActorLogging {

  // We need to know when the connection dies without sending a `Tcp.ConnectionClosed`
  context.watch(connection)

  def receive: Receive = {
    case Tcp.Received(data) =>
      val text = data.utf8String.trim
      log.debug("Received '{}' from remote address {}", text, remote)
      text match {
        case "close" => context.stop(self)
        case _       => sender ! Tcp.Write(data)
      }
    case _: Tcp.ConnectionClosed =>
      log.debug("Stopping, because connection for remote address {} closed", remote)
      context.stop(self)
    case Terminated(`connection`) =>
      log.debug("Stopping, because connection for remote address {} died", remote)
      context.stop(self)
  }
}
