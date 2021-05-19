#!/usr/bin/env bash

set -exu

# Required by minikube
sudo apt update
sudo apt install conntrack

MINIKUBE_VERSION="v1.20.0"

# From https://minikube.sigs.k8s.io/docs/tutorials/continuous_integration/
curl -Lo minikube "https://storage.googleapis.com/minikube/releases/${MINIKUBE_VERSION}/minikube-linux-amd64" && chmod +x minikube && sudo cp minikube /usr/local/bin/ && rm minikube
curl -Lo kubectl "https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl" && chmod +x kubectl && sudo cp kubectl /usr/local/bin/ && rm kubectl

export MINIKUBE_WANTUPDATENOTIFICATION=false
export MINIKUBE_WANTREPORTERRORPROMPT=false
export MINIKUBE_HOME=$HOME
export CHANGE_MINIKUBE_NONE_USER=true
mkdir -p $HOME/.kube
touch $HOME/.kube/config

export KUBECONFIG=$HOME/.kube/config

# --wait=all means the command will wait until apiserver, system_pods,
# default_sa, apps_running, node_ready, kubelet are all ready.
minikube start --driver=docker --addons=ingress --wait=all

# kubectl commands are now able to interact with Minikube cluster
minikube version
minikube addons list
kubectl -n kube-system get deploy | grep dns
