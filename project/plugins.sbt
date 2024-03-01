// Automagically configure scalac options according to the project Scala version
addSbtPlugin(
  "io.github.davidgregory084" % "sbt-tpolecat" % "0.1.20"
)

// Auto format code based on common standards
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

// Test coverage plugin
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")

addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")