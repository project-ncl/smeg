libraryDependencies += { "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value }

addSbtPlugin("com.artima.supersafe" % "sbtplugin" % "1.1.12")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.5.0")
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.31.0")
