import Dependencies._
import ReleaseTransformations._

addCommandAlias("fix", "all compile:scalafix test:scalafix")
addCommandAlias("fixCheck", "; scalafmtCheckAll ; compile:scalafix --check ; test:scalafix --check")

lazy val compileSettings = Seq(
  Compile / compile := (Compile / compile)
    .dependsOn(
      Compile / scalafmtSbt,
      Compile / scalafmtAll
    )
    .value,
  addCompilerPlugin(scalafixSemanticdb),
  (ThisBuild / scalafixDependencies) += "org.scalatest"   %% "autofix"      % Versions.ScalaTestAutofix,
  (ThisBuild / scalafixDependencies) += "com.nequissimus" %% "sort-imports" % Versions.ScalafixSortImports,
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "utf8",
    "-Xlint:missing-interpolator",
    "-Xlint:private-shadow",
    "-Xlint:type-parameter-shadow",
    "-Ywarn-dead-code",
    "-Ywarn-unused"
  ),
  scalacOptions -= "-Xfatal-warnings",
  scalaVersion := Versions.Scala
)

lazy val dependenciesSettings = Seq(
  excludeDependencies ++= excludeDeps,
  libraryDependencies ++= prodDeps ++ testDeps,
  resolvers ++= CustomResolvers.resolvers
)

lazy val publishSettings = Seq(
  Test / publishArtifact := false,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepTask((Docker / publishLocal)),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val testSettings = Seq(
  Test / logBuffered := false,
  Test / parallelExecution := false
)

lazy val root = (project in file("."))
  .settings(
    name := "kafka-dvs-aviation-edge-producer",
    organization := "it.bitrock.dvs"
  )
  .settings(compileSettings: _*)
  .settings(dependenciesSettings: _*)
  .settings(publishSettings: _*)
  .settings(testSettings: _*)

/** sbt-native-packager plugin
  */
enablePlugins(JavaAppPackaging, DockerPlugin, AshScriptPlugin)

dockerBaseImage := "openjdk:8-jre-alpine"
dockerRepository := Option(sys.env.getOrElse("DOCKER_REPOSITORY", "local"))
(Docker / maintainer) := "Bitrock DVS team dvs@bitrock.it"

// Fix for sbt-native-packager 1.3.19
(Docker / daemonUserUid) := None
(Docker / daemonUser) := "root"

// Remove the top level directory for universal package
topLevelDirectory := None
