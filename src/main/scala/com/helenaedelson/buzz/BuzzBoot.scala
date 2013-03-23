/*
 * Copyright (C) 2013 Helena Edelson <http://www.helenaedelson.com>
 */

package com.helenaedelson.buzz

import scala.util.Try
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.gracefulStop
import akka.actor.{ ActorSystem, Props, ActorRef }
import javax.servlet.{ ServletContextListener, ServletContextEvent }

import com.helenaedelson.buzz.InternalTwitterDomain.TwitterSettings
import com.helenaedelson.buzz.InternalBuzzDomain._
import com.helenaedelson.buzz.GeoCodes._

/**
 * @author Helena Edelson
 */

private[buzz] trait Bootable {

  val settings = new Settings()
  import settings._

  var ordered: IndexedSeq[ActorRef] = IndexedSeq.empty

  implicit val system = ActorSystem(SystemName)

  protected val log = akka.event.Logging(system, system.name)

  system.registerOnTermination(ordered foreach (gracefulShutdown(_)))

  val twitterSettings = TwitterSettings(settings)

  val searches = SearchChannels.map(SearchQuery(_, coordinates, SearchRadius, SearchRadiusUnit))

  val dapi = system.actorOf(Props(new CassandraApi(settings)), "data-api")

  def gracefulShutdown(child: ActorRef): Unit =
    Try(Await.result(gracefulStop(child, 60 seconds), 61.seconds))

}

/**
 * Servlet Container Boot
 */
class BuzzServletContextListener extends ServletContextListener with Bootable {
  import twitterSettings._

  override def contextInitialized(event: ServletContextEvent): Unit = try {
    ordered ++= searches map (search ⇒ system.actorOf(Props(new BuzzSearch(settings, config, search, dapi)), search.actor))
  } catch {
    case e: Throwable ⇒ log.info("Error on start up [{}]", e.getMessage)
  }

  override def contextDestroyed(event: ServletContextEvent): Unit = system.shutdown()
}

/**
 * Integration Test Boot
 */
object BuzzApp extends Bootable {
  import twitterSettings._
  import settings._
  import GeoCodes.coordinates

  def main(args: Array[String]): Unit = {
    ordered ++= searches map (search ⇒ system.actorOf(Props(new BuzzSearch(settings, config, search, dapi)), search.actor))
  }
}

