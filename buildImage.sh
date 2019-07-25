sbt -Doverride.akka.version=$1 "rollingRestart / docker:publishLocal"
