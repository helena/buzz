/*
 * Copyright (C) 2013 Helena Edelson <http://www.helenaedelson.com>
 */

package com.helenaedelson.buzz

import scala.util.Try
import scala.collection.JavaConversions._
import com.netflix.astyanax.{ Keyspace, AstyanaxContext }
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl
import com.netflix.astyanax.connectionpool.NodeDiscoveryType
import com.netflix.astyanax.connectionpool.impl.{ CountingConnectionPoolMonitor, ConnectionPoolConfigurationImpl }
import com.netflix.astyanax.thrift.ThriftFamilyFactory
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException
import com.netflix.astyanax.model.{ ColumnFamily, ColumnList }
import com.netflix.astyanax.serializers.StringSerializer
import akka.actor.{ ActorLogging, Actor }
import com.helenaedelson.buzz.UserPersistAction.{ PersistEnvelope, Tweet, GetTweets }

/**
 * @author Helena Edelson
 */
trait DataApi extends Actor with ActorLogging {

  def settings: Settings

}

private[helenaedelson] class CassandraApi(val settings: Settings) extends DataApi {
  import Cassandra._
  import ColumnFamilies._
  import settings._

  val cassandraContext = CassandraContext(settings)

  /**
   * Shuts down the storage instance and cleans up the cassandra context
   */
  override def preStart(): Unit = cassandraContext.start()

  /**
   * Shuts down the storage instance and cleans up the cassandra context
   */
  override def postStop(): Unit = cassandraContext.shutdown()

  def receive: Receive = {
    case PersistEnvelope(tweet) ⇒ addTweet(tweet)
    case GetTweets              ⇒ findAllTweets()
  }

  def addTweet(tweet: Tweet): Unit = {
    val mutation = cassandraContext.getEntity.prepareMutationBatch()

    mutation.withRow(Tweets, tweet.id)
      .putColumn("username", tweet.username, null)
      .putColumn("text", tweet.text, null)
      .putColumn("keyword", tweet.keyword, null)
      .putColumn("latitude", tweet.latitude, null)
      .putColumn("longitude", tweet.longitude, null)
      .putColumn("is_retweet", tweet.isRetweet, null)
      .putColumn("user_image", tweet.imageUri match {
        case Some(uri) ⇒ uri; case None ⇒ ""
      }, null)
      .putColumn("image_uri", tweet.imageUri match {
        case Some(uri) ⇒ uri; case None ⇒ ""
      }, null)

    Try(mutation.execute()) recoverWith {
      case e: ConnectionException ⇒
        log.error("Failed to persist %s" format tweet)
        throw e
    }
  }

  def findAllTweets(): Unit = {
    val res = cassandraContext.getEntity.prepareQuery(Tweets).getKey(CassandraTweetKey).execute()
    val columns: ColumnList[String] = res.getResult
    val id = columns.getColumnByName("id").getStringValue
    val user = columns.getColumnByName("username").getStringValue
    val text = columns.getColumnByName("text").getStringValue
    val keyword = columns.getColumnByName("keyword").getStringValue
    val latitude = columns.getColumnByName("latitude").getStringValue
    val longitude = columns.getColumnByName("latitude").getStringValue
    val isRetweet = columns.getColumnByName("is_retweet").getBooleanValue
    val userImage = columns.getColumnByName("user_image").getStringValue
    val imageUri = columns.getColumnByName("image_uri").getStringValue
    val tweet = Tweet(id, user, text, keyword, latitude, longitude, isRetweet,
      if (userImage.nonEmpty) Some(userImage) else None, if (imageUri.nonEmpty) Some(imageUri) else None)
    log.info("Retrieved {}", tweet)
  }

  def columnsForTweets(key: String): Iterable[String] = getSchemaFor(Tweets, CassandraTweetKey)

  private def getSchemaFor(columnFamily: ColumnFamily[String, String], key: String): Iterable[String] = {
    val result = cassandraContext.getEntity.prepareQuery(columnFamily).getKeySlice(Seq(key)).execute().getResult
    result.flatMap(row ⇒ row.getColumns.getColumnNames)
  }
}

private[buzz] object Cassandra {

  object ColumnFamilies {
    val Tweets = new ColumnFamily[String, String]("tweets", StringSerializer.get(), StringSerializer.get())
  }

  case class CassandraContext(underlyingContext: AstyanaxContext[Keyspace])

  object CassandraContext {

    def apply(settings: Settings): AstyanaxContext[Keyspace] = {
      import settings._

      new AstyanaxContext.Builder()
        .forCluster(CassandraClusterName)
        .forKeyspace(CassandraKeyspaceName)
        .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
          .setDiscoveryType(NodeDiscoveryType.NONE)).withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("MyConnectionPool")
          .setPort(CassandraPort)
          .setMaxConnsPerHost(CassandraMaxConnectionsPerNode)
          .setSeeds(CassandraSeedNodes)).withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
        .buildKeyspace(ThriftFamilyFactory.getInstance())
    }
  }
}

