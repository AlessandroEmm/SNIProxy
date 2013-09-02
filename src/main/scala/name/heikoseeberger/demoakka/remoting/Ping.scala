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
package remoting

import akka.actor.{ Actor, ActorLogging, ActorSelection, PoisonPill, Props }
import scala.concurrent.duration._

object Ping {

  def props(other: ActorSelection): Props =
    Props(new Ping(other))
}

class Ping(other: ActorSelection) extends Actor with ActorLogging {

  import context.dispatcher

  context.system.scheduler.schedule(0 seconds, 1 second, self, "SEND_PING")
  context.system.scheduler.scheduleOnce(10 seconds, self, "SHUTDOWN")

  override def receive: Receive = {
    case "PING"      => log.info("Received 'PING' from '{}'", sender.path)
    case "SEND_PING" => other ! "PING"
    case "SHUTDOWN"  => context.system.shutdown()
  }
}
