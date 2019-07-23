# Akka Upgrade Testing

Tests to show that rolling upgrades that upgrade Akka function.

* Changes to serialisation

Each time a new Akka version is released a new version of this is published
as a docker image. 

A cluster is started in `mimikube` and for version in a list is is half upgraded,
tests run to make sure there is communication between nodes and then the upgrade
is completed.

TODO:
* Endpoint for akka version
