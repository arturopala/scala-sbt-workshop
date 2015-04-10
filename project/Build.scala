import sbt._
import Keys._

object CommonSettings {
	val scalaVersion = "2.11.6"
}

object MyBuild extends Build {

	val main = Project("main", file("."))
		.enablePlugins(MyPlugin)
		.settings(
   			version := "0.1.a",
   			organization := "fi.siili",
    		scalaVersion := CommonSettings.scalaVersion,
    		MyKeys.mySetting := version.value
		)
}

object MyKeys {
	val mySetting = settingKey[String]("A new my setting.")
	val myTask = taskKey[String]("A new my task.")
}

object MyPlugin extends AutoPlugin {

	val MyScope = config("MyScope")

    import MyKeys._

	def mySettings = Seq(
		myTask in MyScope := {
			val a = ((mySetting in MyScope) ?? "Missing").value
			println(a)
			a.reverse
		}
	)

	override def projectSettings: Seq[Setting[_]] = mySettings

}