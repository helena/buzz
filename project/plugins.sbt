
resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Web plugin repo" at "http://siasia.github.com/maven2"
)

addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.0.0")

libraryDependencies <+= sbtVersion(v => v match {
  case x if (x.startsWith("0.12")) => "com.github.siasia" %% "xsbt-web-plugin" % "0.12.0-0.2.11.1"
})
