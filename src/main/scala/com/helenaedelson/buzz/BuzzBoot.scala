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
import util.control.NonFatal
import com.helenaedelson.buzz.GeoCodes._

/**
 * @author Helena Edelson
 */
object BuzzApp extends Bootable {
  import settings._
  import GeoCodes.coordinates

  def main(args: Array[String]): Unit = {
    val twitterSettings = TwitterSettings(settings)
    import twitterSettings._

    val searches: Set[SearchQuery] = SearchChannels.map(SearchQuery(_, coordinates, SearchRadius, SearchRadiusUnit))

    ordered ++= searches map (search ⇒ system.actorOf(Props(new BuzzSearch(settings, config, search, dapi)), search.actor))

  }
}

private[buzz] trait Bootable {

  val settings = new Settings()
  import settings._
  import GeoCodes._

  var ordered: IndexedSeq[ActorRef] = IndexedSeq.empty

  /**
   * The ActorSystem the application will use.
   */
  implicit val system = ActorSystem(SystemName)

  protected val log = akka.event.Logging(system, system.name)

  system.registerOnTermination(ordered foreach (gracefulShutdown(_)))

  val dapi = system.actorOf(Props(new CassandraApi(settings)), "data-api")

  def gracefulShutdown(child: ActorRef): Unit =
    Try(Await.result(gracefulStop(child, 60 seconds), 61.seconds))

}

/**
 * Servlet Container
 */
class BuzzServletContextListener extends ServletContextListener with Bootable {
  import settings._

  val twitterSettings = TwitterSettings(settings)
  import twitterSettings._

  val searches: Set[SearchQuery] = SearchChannels.map(SearchQuery(_, coordinates, SearchRadius, SearchRadiusUnit))

  override def contextInitialized(event: ServletContextEvent): Unit = try {
    ordered ++= searches map (search ⇒ system.actorOf(Props(new BuzzSearch(settings, config, search, dapi)), search.actor))
  } catch {
    case e: Throwable ⇒ log.info("Error on start up [{}]", e.getMessage)
  }

  override def contextDestroyed(event: ServletContextEvent): Unit = system.shutdown()

}
