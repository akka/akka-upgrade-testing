#!/bin/bash

eval $(minikube -p minikube docker-env)
sbt -Doverride.akka.version=$1 "docker:publishLocal"
