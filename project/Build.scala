import sbt._
import Keys._

object CommonSettings {
	val scalaVersion = "2.11.6"
}

object MyBuild extends Build {

	val main = Project("main", file("."))
		.settings(
   			version := "0.1.a",
   			organization := "fi.siili",
    		scalaVersion := CommonSettings.scalaVersion,
    		MyPlugin.dumbSetting := "Scala source: "+(scalaSource in Compile).value.toString
		)
		.enablePlugins(MyPlugin)
}

object MyPlugin extends AutoPlugin {

    val dumbSetting = settingKey[String]("A new setting 01.")
	val dumbTask = taskKey[Unit]("A new task 01.")

	def dumbSettings = Seq(
		dumbTask := println(dumbSetting.value),
		(compile in Compile) <<= (compile in Compile).dependsOn(dumbTask)
	)

	override def projectSettings: Seq[Setting[_]] = dumbSettings

}