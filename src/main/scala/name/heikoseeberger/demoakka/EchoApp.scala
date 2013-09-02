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

import akka.actor.{ Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy }
import akka.actor.ActorDSL._
import scala.concurrent.duration._
import scala.util.Properties.{ lineSeparator => newLine }

object EchoApp extends App {

  val system = ActorSystem("demo-system")
  system.actorOf(Props(new Parent), "parent")

  readLine(s"Hit ENTER to exit ...$newLine")
  system.shutdown()
}

class Parent extends Actor {

  val echo = context.actorOf(Props(new Echo), "echo")

  override val supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(loggingEnabled = false) {
      case e if e.getMessage == "HARMLESS" => SupervisorStrategy.Resume
      case e if e.getMessage == "SEVERE"   => SupervisorStrategy.Restart
      case e if e.getMessage == "KILL"     => SupervisorStrategy.Stop
    }

  actor(new Act with ActorLogging {
    echo ! "HARMLESS"
    echo ! "Still alive after HARMLESS"
    echo ! "SEVERE"
    echo ! "Still alive after SEVERE"
    echo ! "KILL"
    echo ! "Still alive after KILL"
    become {
      case message =>
        log.info(message.toString)
    }
  })

  override def receive: Receive =
    Actor.emptyBehavior
}

class Echo extends Actor {

  override def receive: Receive = {
    case "HARMLESS" => sys.error("HARMLESS")
    case "SEVERE"   => sys.error("SEVERE")
    case "KILL"     => sys.error("KILL")
    case message    => sender ! message
  }
}
