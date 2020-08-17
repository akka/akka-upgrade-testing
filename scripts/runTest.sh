#!/usr/bin/env bash

# Copyright (C) Lightbend Inc. <https://www.lightbend.com>

## Search for snapshots is harcdoded to scala 2.13
## Queries differ because the artifact names changed during the 2.6 milestones phase.
AKKA_25_SNAPSHOT=$(curl -s https://repo.akka.io/snapshots/com/typesafe/akka/akka-actor_2.13/ | grep -oEi '2\.5-[0-9]{8}-[0-9]{6}' | sort -V | tail -n 1)
AKKA_26_SNAPSHOT=$(curl -s https://repo.akka.io/snapshots/com/typesafe/akka/akka-actor_2.13/ | grep -oEi '2\.6\.[0-9]+\+[0-9]+-[0-9a-f]{8}' | sort -V | tail -n 1)

echo "Using Akka SNAPSHOTs [2.5: ${AKKA_25_SNAPSHOT}, 2.6: ${AKKA_26_SNAPSHOT}]"

OPTS="-Dbuild.akka.25.snapshot=${AKKA_25_SNAPSHOT} -Dbuild.akka.26.snapshot=${AKKA_26_SNAPSHOT}"

eval $(minikube -p minikube docker-env)
sbt $OPTS "it:test" 



