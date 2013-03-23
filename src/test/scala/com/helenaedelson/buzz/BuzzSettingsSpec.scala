/*
 * Copyright (C) 2013 Helena Edelson <http://www.helenaedelson.com>
 */

package com.helenaedelson.buzz

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.{ BeforeAndAfterAll, WordSpec }
import org.scalatest.matchers.MustMatchers

class BuzzSettingsSpec extends TestKit(ActorSystem(Settings.SystemName))
  with WordSpec with MustMatchers with BeforeAndAfterAll {

  override def afterAll() { system.shutdown() }

  "Search" must {
    "work" in {
      val settings = new Settings()
      import settings._

      SystemName must be("Buzz")
      SearchRadius must be(50.0)
      SearchRadiusUnit must be("mi")
      SearchChannels.size must be > (0)

      /* oath */
      TwitterConsumerKey.size must be(22)
      TwitterConsumerSecret.size must be > (22)
      TwitterAccessToken.size must be > (22)
      TwitterAccessTokenSecret.size must be > (22)

      CassandraSeedNodes must be("10.30.65.66")
      CassandraPort must be(9160)
      CassandraMaxConnectionsPerNode must be(10)
      CassandraClusterName must be("Test Cluster")
      CassandraKeyspaceName must be("buzz")

      system.shutdown()
    }
  }
}