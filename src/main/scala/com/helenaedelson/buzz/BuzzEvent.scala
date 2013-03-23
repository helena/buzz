/*
 * Copyright (C) 2013 Helena Edelson <http://www.helenaedelson.com>
 */

package com.helenaedelson.buzz

import twitter4j.conf.{ ConfigurationBuilder, Configuration }
import twitter4j.Status
import com.helenaedelson.buzz.InternalBuzzDomain.Geo

/**
 * @author Helena Edelson
 */
sealed trait BuzzEvent

sealed trait BuzzDomainEvent extends BuzzEvent

private[buzz] object InternalSearchAction {

  case object Search extends BuzzEvent

}

private[buzz] object InternalBuzzDomain {

  case class Geo(lat: Double, long: Double, name: String)

  case class Channel(primary: String, synonyms: Set[String])

  case class SearchQuery(channel: Channel, locations: Set[Geo], radius: Double, unit: String, actor: String)

  object SearchQuery {

    def apply(keyword: String, locations: Set[Geo], radius: Double, unit: String): SearchQuery =
      SearchQuery(createChannel(keyword), locations, radius, unit: String, actor(keyword))

    // eventually via db
    def createChannel(keyword: String): Channel = {
      val synonyms: Set[String] = keyword match {
        case word if word.endsWith("e") ⇒ // hike, bike
          Set(word + "r") // and strip r, and ing
        case word if word.endsWith("ing") ⇒ // climbing, kayaking
          Set.empty // strip ing, add er
        case word if !word.endsWith("ing") ⇒ // climb, ski
          Set(word + "ing")
        case _ ⇒ Set.empty
      }
      Channel(keyword, synonyms)
    }

    def actor(keyword: String): String = if (keyword.contains("#")) stripTag(keyword) else keyword

    def stripTag(keyword: String): String = keyword match {
      case word if isHashTag(word) ⇒ keyword.substring(keyword.lastIndexOf('#') + 1)
      case word                    ⇒ keyword
    }

    def isHashTag(keyword: String): Boolean = keyword contains "#"
  }
}

object UserPersistAction {

  @SerialVersionUID(0L)
  case class PersistEnvelope(tweet: Tweet)

  @SerialVersionUID(0L)
  case class Tweet(id: String, username: String, text: String, keyword: String,
                   latitude: String, longitude: String, isRetweet: Boolean,
                   userImage: Option[String], imageUri: Option[String])

  object Tweet {

    def apply(keyword: String, geo: Geo, status: Status): Tweet =
      Tweet(status.getId.toString, status.getUser.getScreenName, status.getText, keyword,
        geo.lat.toString, geo.long.toString,
        status.isRetweet, Tweet.userImageUri(status), Tweet.mediaImageUri(status))

    def userImageUri(status: Status): Option[String] =
      if (status.getUser.getProfileImageURL.nonEmpty) Some(status.getUser.getProfileImageURL) else None

    def mediaImageUri(status: Status): Option[String] =
      if (status.getMediaEntities.nonEmpty) Some(status.getMediaEntities.head.getMediaURL) else None
  }

  case object GetTweets
}

private[buzz] object InternalTwitterDomain {

  case class TwitterSettings(config: Configuration)

  case object TwitterSettings {

    def apply(settings: Settings): TwitterSettings = {
      import settings._

      TwitterSettings(new ConfigurationBuilder()
        .setJSONStoreEnabled(true)
        .setDebugEnabled(TwitterDebugEnabled)
        .setOAuthConsumerKey(TwitterConsumerKey)
        .setOAuthConsumerSecret(TwitterConsumerSecret)
        .setOAuthAccessToken(TwitterAccessToken)
        .setOAuthAccessTokenSecret(TwitterAccessTokenSecret)
        .setLoggerImpl(TwitterLoggerFQCN)
        .build())
    }
  }

}

