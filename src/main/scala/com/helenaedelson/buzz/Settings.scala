/*
 * Copyright (C) 2013 Helena Edelson <http://www.helenaedelson.com>
 */

package com.helenaedelson.buzz

import scala.util.Try
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.collection.JavaConversions._
import com.typesafe.config.ConfigFactory
import twitter4j.Query
import java.util.concurrent.TimeUnit.MILLISECONDS
import com.helenaedelson.buzz.InternalBuzzDomain.Geo

/**
 * @author Helena Edelson
 */
class Settings {

  import Settings._

  private val conf = ConfigFactory.load().getConfig("buzz")

  final val SystemName = conf.getString("app.name")

  final val SearchChannels: Set[String] = conf.getStringList("search.keywords").toSet

  final val SearchRadius: Double = conf.getDouble("search.radius")

  final val SearchRadiusUnit: String = conf.getString("search.unit") match {
    case "mi" ⇒ Query.MILES
    case "km" ⇒ Query.KILOMETERS
  }

  final val SearchTaskInterval: FiniteDuration = Duration(conf.getMilliseconds("search.search-task-interval"), MILLISECONDS)

  final val SearchQueryCount: Int = conf.getInt("search.query.count")

  final val TwitterDebugEnabled: Boolean = conf.getBoolean("twitter.debug-enabled")

  final val TwitterConsumerKey: String = environment("TWITTER_CONSUMER_KEY") match {
    case Some(variable) ⇒ variable
    case None           ⇒ conf.getString("twitter.oauth.consumerKey")
  }

  final val TwitterConsumerSecret: String = environment("TWITTER_CONSUMER_SECRET") match {
    case Some(variable) ⇒ variable
    case None           ⇒ conf.getString("twitter.oauth.consumerSecret")
  }

  final val TwitterAccessToken: String = environment("TWITTER_ACCESS_TOKEN") match {
    case Some(variable) ⇒ variable
    case None           ⇒ conf.getString("twitter.oauth.accessToken")
  }

  final val TwitterAccessTokenSecret: String = environment("TWITTER_ACCESS_TOKEN_SECRET") match {
    case Some(variable) ⇒ variable
    case None           ⇒ conf.getString("twitter.oauth.accessTokenSecret")
  }

  final val TwitterLoggerFQCN: String = conf.getString("twitter.logger-fqcn")

  final val CassandraSeedNodes: String = conf.getString("cassandra.seeds")

  final val CassandraPort: Int = conf.getInt("cassandra.port")

  final val CassandraMaxConnectionsPerNode: Int = conf.getInt("cassandra.max-connections-per-node")

  final val CassandraClusterName: String = conf.getString("cassandra.cluster-name")

  final val CassandraKeyspaceName: String = conf.getString("cassandra.keyspace-name")

  final val CassandraTweetKey: String = conf.getString("cassandra.tweet-key")

}

object Settings {

  final val SystemName = ConfigFactory.load().getString("buzz.app.name")

  def environment(name: String): Option[String] =
    Try(System.getenv(name)).toOption
}

/**
 * For fast prototyping vs mathematical
 */
object GeoCodes {

  val coordinates = Set(
    Geo(61.1919, -149.7621, "Anchorage, AK, US"),
    Geo(62.3114, -150.0869, "Talkeetna, AK, US"),
    Geo(64.8378, -147.7164, "Fairbanks, AK, US"),
    Geo(47.6097, -122.3331, "Seattle, WA, US"),
    Geo(45.5236, -122.675, "Portland, OR, US"),
    Geo(37.775, -122.4183, "San Francisco, CA, US"),
    Geo(37.738006, -119.578543, "Yosemite, CA, US"),
    Geo(34.0522, -118.2428, "Los Angeles, CA, US"),
    Geo(32.7153, -117.1564, "San Diego, CA, US"),
    Geo(36.1208, -115.1722, "Las Vegas, NV, US"),
    Geo(40.75, -111.8833, "Salt Lake City, UT, US"),
    Geo(43.48, -110.7617, "Jackson Hole, WY, US"),
    Geo(46.8722, -113.9931, "Missoula, MT, US"),
    /*Geo(40.7142, -74.0064, "New York, NY, US"),*/
    Geo(44.0536, -71.1289, "North Conway, NH, US"),
    Geo(40.015, -105.27, "Boulder, CO, US"),
    Geo(38.56, -109.55, "Moab, UT, US"),
    Geo(51.1729, -115.5679, "Banff, Alberta, Canada"),
    Geo(50.1244, -122.96, "Whistler, BC, Canada"),
    Geo(45.9189, 6.8653, "Chamonix, France"),
    Geo(47.369, 8.538, "Zurich, Switzerland"),
    Geo(27.7167, 85.3667, "Kathmandu, Nepal"),
    Geo(-49.3289, -72.93, "El Chalten, Argentina"),
    Geo(-54.8, -68.3, "Ushuaia, Argentina"),
    Geo(-9.5308, -77.5281, "Huaraz, Peru"))
}
