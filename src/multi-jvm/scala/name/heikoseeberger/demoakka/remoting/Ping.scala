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

import akka.actor._
import com.typesafe.config.ConfigFactory

object PingMultiJvmNode01 extends App with PingApp {
  def thisNode = "node01"
  def otherNode = "node02"
  def otherPort = 10002
}

object PingMultiJvmNode02 extends App with PingApp {
  def thisNode = "node02"
  def otherNode = "node01"
  def otherPort = 10001
}

trait PingApp {

  def thisNode: String
  def otherNode: String
  def otherPort: Int

  val config = {
    val overrides = ConfigFactory.defaultOverrides()
    val remote = ConfigFactory.parseResourcesAnySyntax("application-remote")
    overrides.withFallback(remote).withFallback(ConfigFactory.load())
  }
  val system = ActorSystem(s"$thisNode-system", config)
  val other = system.actorSelection(s"akka.tcp://$otherNode-system@localhost:$otherPort/user/ping")
  val ping = system.actorOf(Ping.props(other), "ping")

  println(s"'$thisNode-system' awaiting termination ...")
  system.awaitTermination()
  println(s"'$thisNode-system' terminated!")
}
