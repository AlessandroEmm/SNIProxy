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
import spray.can.Http
import spray.http.{ HttpRequest, HttpResponse }
import spray.http.HttpMethods._
import scala.util.Properties.{ lineSeparator => newLine }

object HttpEchoServiceApp extends App {

  val system = ActorSystem("demo-system")
  system.actorOf(HttpEchoService.props("localhost", 8080), "http-echo-service")

  readLine(s"Hit ENTER to exit ...$newLine")
  system.shutdown()
}

object HttpEchoService {

  def props(host: String, port: Int): Props =
    Props(new HttpEchoService(host, port))
}

class HttpEchoService(host: String, port: Int) extends Actor with ActorLogging {

  import context.system

  IO(Http) ! Http.Bind(self, host, port)

  override def receive: Receive = {
    case Http.Connected(remote, _) =>
      log.debug("Remote address {} connected", remote)
      sender ! Http.Register(context.actorOf(EchoConnectionHandler.props(remote, sender)))
  }
}

object EchoConnectionHandler {

  def props(remote: InetSocketAddress, connection: ActorRef): Props =
    Props(new EchoConnectionHandler(remote, connection))
}

class EchoConnectionHandler(remote: InetSocketAddress, connection: ActorRef) extends Actor with ActorLogging {

  // We need to know when the connection dies without sending a `Tcp.ConnectionClosed`
  context.watch(connection)

  def receive: Receive = {
    case HttpRequest(GET, uri, _, _, _) =>
      sender ! HttpResponse(entity = uri.path.toString)
    case _: Tcp.ConnectionClosed =>
      log.debug("Stopping, because connection for remote address {} closed", remote)
      context.stop(self)
    case Terminated(`connection`) =>
      log.debug("Stopping, because connection for remote address {} died", remote)
      context.stop(self)
  }
}
