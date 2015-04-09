Scala SBT Workshop
===

Step 1 - Project configuration
---

    git clone https://github.com/arturopala/scala-sbt-workshop.git
    sbt

Initial setup consists of `/project/Build.scala` file only.

Exercise: Test default project settings using `inspect` and `show` commands.

    > show projectId
    > show sourceDirectories
    > inspect configuration

### Define project

Exercise: Use [`Project`](https://github.com/sbt/sbt/blob/0.13/main/src/main/scala/sbt/Project.scala#L194) object to create new project(s)

    object Project extends ProjectExtra {
      def apply(
          id: String, 
          base: File, 
          aggregate: => Seq[ProjectReference] = Nil, 
          dependencies: => Seq[ClasspathDep[ProjectReference]] = Nil,
          delegates: => Seq[ProjectReference] = Nil, 
          settings: => Seq[Def.Setting[_]] = Nil, 
          configurations: Seq[Configuration] = Nil,
          auto: AddSettings = AddSettings.allDefaults): Project = ...
    }

returning instance of a [`Project`](https://github.com/sbt/sbt/blob/0.13/main/src/main/scala/sbt/Project.scala#L85) trait

    sealed trait Project extends ProjectDefinition[ProjectReference] {
        def copy ...
        def configure(transforms: (Project => Project)*): Project = ...
        def in(dir: File): Project = copy(base = dir)
        def overrideConfigs(cs: Configuration*): Project = copy(...)
        def configs(cs: Configuration*): Project = copy(configurations = configurations ++ cs)
        def dependsOn(deps: ClasspathDep[ProjectReference]*): Project = copy(dependencies = dependencies ++ deps)
        def aggregate(refs: ProjectReference*): Project = copy(aggregate = (aggregate: Seq[ProjectReference]) ++ refs)
        def settings(ss: SettingsDefinition*): Project = copy(settings = (settings: Seq[Setting[_]]) ++ ss.flatMap(_.settings))
        def settingSets(select: AddSettings*): Project = copy(auto = AddSettings.seq(select: _*))
        def addSbtFiles(files: File*): Project = copy(auto = AddSettings.append(auto, AddSettings.sbtFiles(files: _*)))
        def enablePlugins(ns: Plugins*): Project = ...
        def disablePlugins(ps: AutoPlugin*): Project = ...
    }

*Remember:* Everything in sbt project definition is **IMMUTABLE**

    > inspect projectId

### Add settings

    git checkout step1

Exercise: Customize project settings ([name](https://github.com/sbt/sbt/blob/0.13/main/src/main/scala/sbt/Keys.scala#L212), [version](https://github.com/sbt/sbt/blob/0.13/main/src/main/scala/sbt/Keys.scala#L287), [organization](https://github.com/sbt/sbt/blob/0.13/main/src/main/scala/sbt/Keys.scala#L218), description, etc.) using `.settings(...)` method and predefined [keys](https://github.com/sbt/sbt/blob/0.13/main/src/main/scala/sbt/Keys.scala).

##### How that works?

Settings are represented as [`SettingsDefinition`](https://github.com/sbt/sbt/blob/0.13/util/collection/src/main/scala/sbt/Settings.scala#L446) wrappers of sequence of `Setting`:

    sealed trait SettingsDefinition {
        def settings: Seq[Setting[_]]
      }

##### What is setting?

Each [`Setting`](https://github.com/sbt/sbt/blob/0.13/util/collection/src/main/scala/sbt/Settings.scala#L454) consists of *key* and *init* function:

    sealed class Setting[T] private[Init] (
        val key: ScopedKey[T], 
        val init: Initialize[T], 
        val pos: SourcePosition
    ) extends SettingsDefinition {
        def settings = this :: Nil
        ...
    }

Key of type [`ScopedKey`](https://github.com/sbt/sbt/blob/0.13/util/collection/src/main/scala/sbt/Settings.scala#L45) is just a pair of [`Scope`](https://github.com/sbt/sbt/blob/0.13/main/settings/src/main/scala/sbt/Scope.scala#L9) and [`AttributeKey`](https://github.com/sbt/sbt/blob/0.13/util/collection/src/main/scala/sbt/Attributes.scala#L17):

    final case class ScopedKey[T](scope: Scope, key: AttributeKey[T]) ...

Parameter of type [`Initialize`](https://github.com/sbt/sbt/blob/0.13/util/collection/src/main/scala/sbt/Settings.scala#L413) represents delayed evaluation of a value as a function of the scope settings (scope context):

    sealed trait Initialize[T] {
        def dependencies: Seq[ScopedKey[_]]
        def evaluate(map: Settings[Scope]): T
        ...
    }

    sealed trait Settings[Scope] {
      def data: Map[Scope, AttributeMap]
      def keys(scope: Scope): Set[AttributeKey[_]]
      def scopes: Set[Scope]
      def definingScope(scope: Scope, key: AttributeKey[_]): Option[Scope]
      def allKeys[T](f: (Scope, AttributeKey[_]) => T): Seq[T]
      def get[T](scope: Scope, key: AttributeKey[T]): Option[T]
      def getDirect[T](scope: Scope, key: AttributeKey[T]): Option[T]
      def set[T](scope: Scope, key: AttributeKey[T], value: T): Settings[Scope]
    }

##### What is scope?

[`Scope`](https://github.com/sbt/sbt/blob/0.13/main/settings/src/main/scala/sbt/Scope.scala#L9) is a kind of complex 3-dimensional key:

    final case class Scope(
        project: ScopeAxis[Reference], 
        config: ScopeAxis[ConfigKey], 
        task: ScopeAxis[AttributeKey[_]], 
        extra: ScopeAxis[AttributeMap]) {...}

with 2 predefined values: `This` and `Global`:

    object Scope {
        val ThisScope = Scope(This, This, This, This)
        val GlobalScope = Scope(Global, Global, Global, Global)
        ...
    }

Dimensions of a scope are represented as a [`ScopeAxis`](https://github.com/sbt/sbt/blob/0.13/main/settings/src/main/scala/sbt/ScopeAxis.scala#L5):

    sealed trait ScopeAxis[+S] {
      def fold[T](f: S => T, ifGlobal: => T, ifThis: => T): T = this match {
        case This      => ifThis
        case Global    => ifGlobal
        case Select(s) => f(s)
      }
      ...
    }

There exists 2 predefined values of ScopeAxis: `This` and `Global`, plus one `Select` wrapper.

    case object This extends ScopeAxis[Nothing]
    case object Global extends ScopeAxis[Nothing]
    final case class Select[S](s: S) extends ScopeAxis[S] {...}

First scope dimension is [`Reference`](https://github.com/sbt/sbt/blob/0.13/main/settings/src/main/scala/sbt/Reference.scala#L12) which identifies a project or a build:

    sealed trait Reference
    sealed trait ResolvedReference extends Reference
    sealed trait BuildReference extends Reference
    final case object ThisBuild extends BuildReference
    final case class BuildRef(build: URI) extends BuildReference with ResolvedReference
    sealed trait ProjectReference extends Reference
    final case class ProjectRef(build: URI, project: String) extends ProjectReference with ResolvedReference
    final case class LocalProject(project: String) extends ProjectReference
    final case class RootProject(build: URI) extends ProjectReference
    final case object LocalRootProject extends ProjectReference
    final case object ThisProject extends ProjectReference

Second scope dimension is [`ConfigKey`](https://github.com/sbt/sbt/blob/0.13/main/settings/src/main/scala/sbt/ConfigKey.scala#L3) which identifies a named configuration:

    final case class ConfigKey(name: String)

Third scope dimesion is Task represented as `AttributeKey`.

##### What is key?

Let's go now back to the `ScopedKey` second parameter:

[`AttributeKey`](https://github.com/sbt/sbt/blob/0.13/util/collection/src/main/scala/sbt/Attributes.scala#L17) represents some named build attribute of expected type `T`:

    sealed trait AttributeKey[T] {
      def label: String
      def description: Option[String]
      def extend: Seq[AttributeKey[_]]
      ...
    }

Mostly used flavor of key is [`SettingKey`](https://github.com/sbt/sbt/blob/0.13/main/settings/src/main/scala/sbt/Structure.scala#L34) which binds `AttributeKey` with some targeted `Scope`:

    sealed abstract class SettingKey[T] ... {
      def scope: Scope
      val key: AttributeKey[T]

It directly uses `AttributeKey` factory in its companion object:

      object SettingKey {
        def apply[T: Manifest](label: String, description: String, extend1: Scoped, extendN: Scoped*): SettingKey[T] =
          apply(AttributeKey[T](label, description, extendScoped(extend1, extendN)))
        ...
      }

`SettingKey` provides set of convenient transforming functions:

      final def in(scope: Scope): SettingKey[T] ...
      final def :=(v: T): Setting[T]
      final def +=[U](v: U)(implicit a: Append.Value[T, U]): Setting[T] ...
      final def ++=[U](vs: U)(implicit a: Append.Values[T, U]): Setting[T] ...
      final def <+=[V](v: Initialize[V])(implicit a: Append.Value[T, V]): Setting[T] ...
      final def <++=[V](vs: Initialize[V])(implicit a: Append.Values[T, V]): Setting[T] ...
      final def -=[U](v: U)(implicit r: Remove.Value[T, U]): Setting[T] ...
      final def --=[U](vs: U)(implicit r: Remove.Values[T, U]): Setting[T] ...
      final def ~=(f: T => T): Setting[T] ...
    }

Examples of default `SettingKey` definitions:

    val name = SettingKey[String]("name", "Project name.", APlusSetting)
    val normalizedName = SettingKey[String]("normalized-name", "Project name transformed from mixed case and spaces to lowercase and dash-separated.", BSetting)
    val description = SettingKey[String]("description", "Project description.", BSetting)
    val homepage = SettingKey[Option[URL]]("homepage", "Project homepage.", BSetting)
    val startYear = SettingKey[Option[Int]]("start-year", "Year in which the project started.", BMinusSetting)
    val licenses = SettingKey[Seq[(String, URL)]]("licenses", "Project licenses as (name, url) pairs.", BMinusSetting)
    val organization = SettingKey[String]("organization", "Organization/group ID.", APlusSetting)

`APlusSetting` and others are just `Int` rank values for sorting keys when displayed.

##### Put it together

`Keys.version` is one example of a `SettingKey` which is defined as:

    val version = SettingKey[String]("version", "The version/revision of the current module.", APlusSetting)

We can use it then in our build definition with `:=` operator and some `String` value creating new `Setting` instance in `Global` scope:

    val versionSetting:Setting[String] = Keys.version in Global := "0.1.a"
    val main = Project("main", file(".")).settings(versionSetting)

As `Global` scope is default scope so we can omit it and further inline setting instance creation, as below:

    val main = Project("main", file(".")).settings(
      version := "0.1.a"
    )
