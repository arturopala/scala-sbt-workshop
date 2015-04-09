import sbt._
import Keys._

object Release extends Build {

	val main = Project("main", file("."))
}