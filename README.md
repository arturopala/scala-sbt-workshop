Scala SBT Workshop
===

### Prerequisites

Java <https://www.java.com/pl/download/>
SBT <http://www.scala-sbt.org/download.html>

### Preparation

    git clone https://github.com/sbt/sbt.git
    git clone https://github.com/arturopala/scala-sbt-workshop.git
    cd scala-sbt-workshop
    sbt

Initial setup consists of `/project/Build.scala` file only.

Exercise: Test default project settings using `inspect` and `show` commands.

    > show projectId
    > show sourceDirectories
    > inspect configuration

### Step 01 - Define project

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

### Step 02 - Add settings

    git checkout step01

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

Mostly seen flavor of key is a [`SettingKey`](https://github.com/sbt/sbt/blob/0.13/main/settings/src/main/scala/sbt/Structure.scala#L34) which identifies a setting.  It consists of three parts: the scope, the name, and the type of a value associated with this key. The scope is represented by a value of type `Scope`. The name and the type are represented by a value of type `AttributeKey[T]`.

    sealed abstract class SettingKey[T] ... {
      def scope: Scope
      val key: AttributeKey[T]
      ...

Instances are constructed using the companion object directly invoking `AttributeKey` factory:

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

[Read more about settings](http://www.scala-sbt.org/0.13/tutorial/More-About-Settings.html)

### Step 03 - Add tasks

    git checkout step02

Another type of key unnecessary to do some real job in SBT is a [`TaskKey`](https://github.com/sbt/sbt/blob/0.13/main/settings/src/main/scala/sbt/Structure.scala#L66):

    sealed abstract class TaskKey[T] ... {
      def scope: Scope
      val key: AttributeKey[Task[T]]
      ...
    }

`TaskKey` identifies a task.  It consists of three parts: the scope, the name, and the type of the value computed by a task associated with this key. The scope is represented by a value of type `Scope`. The name and the type are represented by a value of type `AttributeKey[Task[T]]`. Instances are constructed using the companion object.

Examples of default `TaskKey` definitions:

    val packagedArtifact = TaskKey[(Artifact, File)]("packaged-artifact", "Generates a packaged artifact, returning the Artifact and the produced File.", CTask)
    val managedSources = TaskKey[Seq[File]]("managed-sources", "Sources generated by the build.", BTask)
    val sources = TaskKey[Seq[File]]("sources", "All sources, both managed and unmanaged.", BTask)

`TaskKey` is similar to `SettingKey` but instead of static value wraps an action generating that value. This difference is like `val` vs. `def` in plain Scala.

##### What is task?

[`Task`](https://github.com/sbt/sbt/blob/0.13/tasks/standard/src/main/scala/sbt/Action.scala#L52) combines metadata [`Info`](https://github.com/sbt/sbt/blob/0.13/tasks/standard/src/main/scala/sbt/Action.scala#L67) and a computation [`Action`](https://github.com/sbt/sbt/blob/0.13/tasks/standard/src/main/scala/sbt/Action.scala#L14).

    final case class Task[T](info: Info[T], work: Action[T]) ...

    sealed trait Action[T]

`Info` object is used to provide information about a task, such as the name, description, and tags for controlling concurrent execution.

    final case class Info[T](
      attributes: AttributeMap = AttributeMap.empty, 
      post: T => AttributeMap = const(AttributeMap.empty)
    ) {
      def name = attributes.get(Name)
      def description = attributes.get(Description)
      ...
    }

##### How to create task setting?

    def <<=(app: Initialize[Task[S]]): Setting[Task[S]] = ...
    def :=(v: S): Setting[Task[S]] = ...
    def ~=(f: S => S): Setting[Task[S]] = ...
    def +=[U](v: U)(implicit a: Append.Value[T, U]): Setting[Task[T]] = ...
    def ++=[U](vs: U)(implicit a: Append.Values[T, U]): Setting[Task[T]] = ...
    def <+=[V](v: Initialize[Task[V]])(implicit a: Append.Value[T, V]): Setting[Task[T]] = ...
    def <++=[V](vs: Initialize[Task[V]])(implicit a: Append.Values[T, V]): Setting[Task[T]] = ...
    def -=[U](v: U)(implicit r: Remove.Value[T, U]): Setting[Task[T]] = ...
    def --=[U](vs: U)(implicit r: Remove.Values[T, U]): Setting[Task[T]] = ...

    git checkout step03

**Remember: All build dependencies in sbt are automatic rather than explicitly declared. If you use a key’s value in another computation, then the computation depends on that key. It just works!**

[Read more about tasks](http://www.scala-sbt.org/0.13/docs/Tasks.html)