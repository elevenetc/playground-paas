#!/bin/bash

CLUSTER_NAME="paas-cluster"
CONFIG_FILE="kind-config.yaml"

echo "Checking if Kind cluster '$CLUSTER_NAME' exists..."

# Check if kind is installed
if ! command -v kind &> /dev/null; then
    echo "Error: kind is not installed. Please install it first:"
    echo "  brew install kind  # on macOS"
    echo "  Or visit: https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
    exit 1
fi

# Check if cluster exists
CONTAINER_NAME="${CLUSTER_NAME}-control-plane"

if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
    echo "Kind cluster '$CLUSTER_NAME' already exists."

    # Check if container is stopped
    if docker ps -a --format '{{.Names}}\t{{.Status}}' | grep "^${CONTAINER_NAME}" | grep -q "Exited"; then
        echo "Starting stopped cluster..."
        docker start ${CONTAINER_NAME} > /dev/null 2>&1
        sleep 3
    fi

    # Verify cluster is healthy
    if kubectl cluster-info --context kind-${CLUSTER_NAME} &> /dev/null; then
        echo "Cluster is healthy and ready."
        exit 0
    else
        echo "Cluster exists but is not healthy. Recreating..."
        kind delete cluster --name ${CLUSTER_NAME}
    fi
fi

# Create the cluster
echo "Creating Kind cluster '$CLUSTER_NAME'..."
if [ -f "$CONFIG_FILE" ]; then
    kind create cluster --config ${CONFIG_FILE}
else
    echo "Warning: ${CONFIG_FILE} not found. Creating cluster with default settings..."
    kind create cluster --name ${CLUSTER_NAME}
fi

# Verify cluster is ready
echo "Waiting for cluster to be ready..."
kubectl wait --for=condition=Ready nodes --all --timeout=60s

echo "Kind cluster '$CLUSTER_NAME' is ready!"
