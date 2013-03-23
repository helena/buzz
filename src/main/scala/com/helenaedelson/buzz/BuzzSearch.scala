/*
 * Copyright (C) 2013 Helena Edelson <http://www.helenaedelson.com>
 */

package com.helenaedelson.buzz

import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import scala.util.Try
import scala.collection.JavaConverters._
import akka.actor.{ ActorRef, Props, ActorLogging, Actor }
import twitter4j._
import twitter4j.conf.Configuration
import com.helenaedelson.buzz.InternalSearchAction._
import com.helenaedelson.buzz.InternalBuzzDomain._
import com.helenaedelson.buzz.UserPersistAction._
import com.helenaedelson.buzz.InternalBuzzDomain.Geo
import com.helenaedelson.buzz.UserPersistAction.PersistEnvelope

/**
 * @author Helena Edelson
 */
class BuzzSearch(settings: Settings, config: Configuration, searchQuery: SearchQuery, dapi: ActorRef) extends Actor with ActorLogging {
  import context.dispatcher
  import searchQuery._
  import settings._

  val twitter = new TwitterFactory(config).getInstance()

  val task = context.system.scheduler.schedule(Duration.Zero, SearchTaskInterval, self, Search)

  val keywords = Set(channel.primary) ++ channel.synonyms

  override def preStart(): Unit = log.info("Starting on {}", self.path)

  override def postStop(): Unit = task.cancel()

  def receive = {
    case Search ⇒ search()
  }

  def search(): Unit = locations foreach (doSearch(_))

  def doSearch(geo: Geo): Unit = {
    keywords foreach { word ⇒
      log.info("Querying keyword {}, geo {}", word, geo)
      val query = new Query(word)
      query.setGeoCode(new GeoLocation(geo.lat, geo.long), radius, unit)
      query.setResultType(Query.MIXED)
      query.count(SearchQueryCount)

      Try {
        val result = twitter.search(query)
        val tweets: Set[Status] = result.getTweets.asScala.toSet
        tweets foreach (status ⇒ dapi ! PersistEnvelope(Tweet(word, geo, status)))
      } recover { case e: Throwable ⇒ log.error("rate limit hit.") } //statusCode=429 Rate limit exceeded
    }
  }
}