import sbt._
import Keys._

object CommonSettings {
	val scalaVersion = "2.11.6"
}

object MyBuild extends Build {

	val main = Project("main", file("."))
		.enablePlugins(MyPlugin1, MyPlugin2)
		.settings(
   			version := "0.1.a",
   			organization := "fi.siili",
    		scalaVersion := CommonSettings.scalaVersion,
    		MyKeys.dumbSetting := "Santa ",
    		MyKeys.dumbTask += "ho ho"
		)
}

object MyKeys {
	val dumbSetting = settingKey[String]("A new setting 01.")
	val dumbTask = taskKey[String]("A new task 01.")
}

object MyPlugin1 extends AutoPlugin {

    import MyKeys._

	def dumbSettings = Seq(
		dumbTask := dumbSetting.value,
		(compile in Compile) <<= (compile in Compile).dependsOn(dumbTask)
	)

	override def projectSettings: Seq[Setting[_]] = dumbSettings

}

object MyPlugin2 extends AutoPlugin {

    import MyKeys._

	def dumbSettings = Seq(
		dumbTask := dumbSetting.value.reverse,
		(compile in Compile) <<= (compile in Compile).dependsOn(dumbTask)
	)

	override def projectSettings: Seq[Setting[_]] = dumbSettings

}