# Akka Upgrade Testing

Tests to show that rolling upgrades that upgrade Akka function.

* Changes to serialisation

A cluster is started in `mimikube` and for versions in a list is half upgraded,
tests run to make sure there is communication between nodes and then the upgrade
is completed.

Future improvements:

* Have tests that exercise:
    * Sharding
    * Singletons
    * Distributed data
    
## Log verification

During each upgrade logs are checked for WARNs and ERRORs to catch any issues with the multi version cluster.

There is a whitelist of logs in `RollingUpgradeSpec` that can be used to allow certain logs.

## Cron job and snapshots

Once a day a Cron job runs the upgrade test with the latest the pre-configured versions as well as the
latest 2.5 and 2.6 nightly snapshots.



## Running locally

Have minikube running and setup docker to point to minikube's VM e.g. run:

```
eval $(minikube docker-env)
```

Modify `akkaVersions` in `RollingUpgradeSpec` to include the versions of Akka under test then run:

```
sbt "It/test"
```


