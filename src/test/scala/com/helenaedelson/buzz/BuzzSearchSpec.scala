/*
 * Copyright (C) 2013 Helena Edelson <http://www.helenaedelson.com>
 */

package com.helenaedelson.buzz

import scala.collection.JavaConverters._
import twitter4j.{ Status, GeoLocation, Query, TwitterFactory }
import com.helenaedelson.buzz.UserPersistAction.{ Tweet, PersistEnvelope }
import com.helenaedelson.buzz.InternalTwitterDomain.TwitterSettings
import com.helenaedelson.buzz.InternalBuzzDomain.{ Geo, SearchQuery }

class BuzzSearchSpec extends AbstractBuzzSpec {
  "Search" must {
    "work" in {
      BuzzApp.main(Array.empty)
      Thread.sleep(10000)
    }
    /*"pull usable queries" in {
      import settings._
      x

      val twitterSettings = TwitterSettings(settings)
      import twitterSettings._

      val searches: Set[SearchQuery] = SearchChannels.map(SearchQuery(_, coordinates, SearchRadius, SearchRadiusUnit))

      val twitter = new TwitterFactory(config).getInstance()

      // for each query (keyword -> coords)
      searches.foreach { s ⇒
        import s._

        val keywords = Set(channel.primary) ++ channel.synonyms // cleanup

        locations.foreach { geo ⇒
          keywords foreach { word ⇒
            // for each keyword/synonym  by lat/long
            val query = new Query(word)
            query.setGeoCode(new GeoLocation(geo.lat, geo.long), radius, unit)
            query.setResultType(Query.MIXED)
            query.count(SearchQueryCount)

            val result = twitter.search(query)
            val tweets: Set[Status] = result.getTweets.asScala.toSet
            tweets foreach (status ⇒ Tweet(word, geo, status))

            if (tweets.nonEmpty) println("**** Found %s tweets for %s at %s".format(tweets.size, word, geo))
          }
        }
      }
    }*/
  }
}
