concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

parallelExecution in Test := false
parallelExecution in IntegrationTest := false
testForkedParallel in Test := false
testForkedParallel in IntegrationTest := false

lazy val commonSettings = Seq(
  version := "0.1-SNAPSHOT",
  organization := "com.example",
  scalaVersion := "2.10.1",
  test in assembly := {}
)

lazy val binding = (project in file("ledger-core-binding"))

lazy val daemon = (project in file("daemon")).dependsOn(binding)
  .settings(commonSettings: _*)
  .settings(
    //mainClass in assembly := Some("co.ledger.wallet.daemon.LedgerWalletDaemon")
  )


cancelable in Global := true