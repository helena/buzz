/*
 * Copyright (C) 2013 Helena Edelson <http://www.helenaedelson.com>
 */

package com.helenaedelson.buzz

import java.util.UUID
import akka.actor.Props
import com.helenaedelson.buzz.UserPersistAction.{ PersistEnvelope, GetTweets, Tweet }

class CassandraApiSpec extends AbstractBuzzSpec {

  val api = system.actorOf(Props(new CassandraApi(settings)), "cassandra-api")

  "CassandraApi" must {
    "perist a tweet" in {
      val tweet = Tweet(UUID.randomUUID().toString, "helenaedelson", "didiiit", "climb", "-11.09", "40.56", false,
        Some("http://foo.com/user.jpg"), Some("http://foo.com/img.jpg"))
      api ! PersistEnvelope(tweet)
    }
    "get data" in {
      import akka.pattern.ask
      api ! GetTweets
    }
  }
}
