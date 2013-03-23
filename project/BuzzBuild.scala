/**
 *  Copyright (C) 2013 Helena Edelson <http://www.helenaedelson.com>
 */

package buzz

import sbt._
import Keys._
import com.github.siasia.WebPlugin.webSettings
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

/**
* @author Helena Edelson
*/ 
object Settings {

  val buildOrganization = "com.helenaedelson.buz"
  val buildVersion      = "0.1-SNAPSHOT"
  val buildScalaVersion = Version.Scala
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion
  )

  import Resolvers._

  val defaultSettings = buildSettings ++ webSettings ++ formatSettings ++ Seq(
    resolvers ++= Seq(typesafeRepo),
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation"),
    parallelExecution in Compile := true,
    parallelExecution in Test := false,
    ivyXML := <dependencies>
      <exclude module="commons-logging"/>
      <exclude module="jasper-runtime"/>
      <exclude module="jasper-compiler"/>
      <exclude module="jsp-2.1"/>
      <exclude module="jsp-api-2.1"/>
      <exclude module="log4j" />
      <exclude module="servlet-api"/>
      <exclude module="slf4j-log4j12"/>
    </dependencies>
  )

  lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test    := formattingPreferences
  )

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
  }
}

object Resolvers {
  val typesafeRepo =  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
}

object Dependencies {
  import Dependency._

  val test = Seq(Test.akkaTestkit, Test.scalaTest)

  val akka = Seq(akkaActor, akkaRemote, akkaCluster, akkaSlf4j)

  val core = Seq(astyanax, guava, jetty, jodaConvert, jodaTime, logback, scalatra, servletApi, slf4j, twitterCore, twitterStream) ++ akka ++ test
}

object Version {
  val Scala = "2.10.1"
  val Akka  = "2.1.2"
}

object Dependency {
  import Version._
 
  // Compile 
  val akkaActor     = "com.typesafe.akka"       %% "akka-actor"                  % Akka     % "compile"
  val akkaRemote    = "com.typesafe.akka"       %% "akka-remote"                 % Akka     % "compile"
  val akkaSlf4j     = "com.typesafe.akka"       %% "akka-slf4j"                  % Akka     % "compile"
  val akkaCluster   = "com.typesafe.akka"       %% "akka-cluster-experimental"   % Akka     % "compile"
  val astyanax      = "com.netflix.astyanax"    %  "astyanax"                    % "1.0.6"  % "compile"
  val bijection     = "com.twitter"             %% "bijection-core"              % "2.9.2"  % "compile"
  val guava         = "com.google.guava"        % "guava"                        % "11.0.2" % "compile"
  val jetty = "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"
  val jodaConvert   = "org.joda"                %  "joda-convert"                % "1.2"    % "compile"
  val jodaTime      = "joda-time"               %  "joda-time"                   % "2.1"    % "compile"
  val logback       = "ch.qos.logback"          %  "logback-classic"             % "1.0.10" % "compile"
  val scalatra      = "org.scalatra"            %% "scalatra"                    % "2.2.0"  % "compile"
  val servletApi    = "org.eclipse.jetty.orbit" % "javax.servlet"                % "3.0.0.v201112011016" % "compile;container" artifacts (Artifact("javax.servlet", "jar", "jar"))
  val slf4j         = "org.slf4j"               %  "slf4j-api"                   % "1.7.4"  % "compile"
  val twitterCore   = "org.twitter4j"           %  "twitter4j-core"              % "3.0.3"  % "compile"
  val twitterStream = "org.twitter4j"           %  "twitter4j-stream"            % "3.0.3"  % "compile"

  object Test {
    val akkaTestkit = "com.typesafe.akka" %% "akka-testkit"   % Akka    % "test"
    val scalaTest   = "org.scalatest"     %% "scalatest"      % "1.9.1" % "test"
  }
}

object BuzzBuild extends Build {
  import java.io.File._
  import Settings._

  lazy val buzz = Project(
    id = "buzz",
    base = file("."),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.core/*,
      mainRunNobootcpSetting,
      testRunNobootcpSetting,
      testNobootcpSetting*/
    )
  )

  val runNobootcp =
    InputKey[Unit]("run-nobootcp", "Runs main classes without Scala library on the boot classpath")

  val mainRunNobootcpSetting = runNobootcp <<= runNobootcpInputTask(Runtime)
  val testRunNobootcpSetting = runNobootcp <<= runNobootcpInputTask(Test)

  def runNobootcpInputTask(configuration: Configuration) = inputTask {
    (argTask: TaskKey[Seq[String]]) => (argTask, streams, fullClasspath in configuration) map { (at, st, cp) =>
      val runCp = cp.map(_.data).mkString(pathSeparator)
      val runOpts = Seq("-classpath", runCp) ++ at
      val result = Fork.java.fork(None, runOpts, None, Map(), true, StdoutOutput).exitValue()
      if (result != 0) sys.error("Run failed")
    }
  }

  val testNobootcpSetting = test <<= (scalaBinaryVersion, streams, fullClasspath in Test) map { (sv, st, cp) =>
    val testCp = cp.map(_.data).mkString(pathSeparator)
    val testExec = "org.scalatest.tools.Runner"
    val testPath = "target/scala-%s/test-classes" format sv
    val testOpts = Seq("-classpath", testCp, testExec, "-R", testPath, "-o")
    val result = Fork.java.fork(None, testOpts, None, Map(), false, LoggedOutput(st.log)).exitValue()
    if (result != 0) sys.error("Tests failed")
  }
}

