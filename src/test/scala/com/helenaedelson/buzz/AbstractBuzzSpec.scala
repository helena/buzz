/*
 * Copyright (C) 2013 Helena Edelson <http://www.helenaedelson.com>
 */

package com.helenaedelson.buzz

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.{ BeforeAndAfterAll, WordSpec }
import org.scalatest.matchers.MustMatchers

abstract class AbstractBuzzSpec extends TestKit(ActorSystem(Settings.SystemName)) with WordSpec with MustMatchers
  with BeforeAndAfterAll {

  override def afterAll() { system.shutdown() }

  val settings = new Settings()
  import settings._

  implicit val timeout = system.settings.CreationTimeout

}
