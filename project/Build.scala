import sbt._
import Keys._

object CommonSettings {
	val scalaVersion = "2.11.6"
}

object Release extends Build {

	val versionSetting:Setting[String] = Keys.version in Global := "0.1.a"

	val main = Project("main", file("."))
		.settings(
   			versionSetting,
   			organization := "fi.siili",
    		scalaVersion := CommonSettings.scalaVersion
		)
}