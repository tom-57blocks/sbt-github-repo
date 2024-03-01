import sbt._

object Common {

  val dependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
    "org.scalatestplus" %% "mockito-3-4" % Versions.scalatestplusMockito % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % Versions.scalatestplus % Test
  )

}
