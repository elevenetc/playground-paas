#!/bin/bash

set -e

echo "=========================================="
echo "  Shutting Down Development Environment"
echo "=========================================="
echo

# Stop Docker Compose services
echo "Step 1: Stopping Docker Compose services..."
docker-compose stop
echo "  ✓ Services stopped"

echo
echo "Step 2: Stopping client..."
if pkill -f "vite" 2>/dev/null; then
    echo "  ✓ Client stopped"
else
    echo "  - Client not running"
fi

echo
echo "Step 3: Stopping kubectl proxy..."
if lsof -ti :8001 | xargs kill 2>/dev/null; then
    echo "  ✓ kubectl proxy stopped"
else
    echo "  - kubectl proxy not running"
fi

echo
echo "Step 4: Stopping Kind cluster..."
CLUSTER_CONTAINER="paas-cluster-control-plane"
if docker ps --format '{{.Names}}' | grep -q "^${CLUSTER_CONTAINER}$"; then
    docker stop ${CLUSTER_CONTAINER} > /dev/null 2>&1
    echo "  ✓ Kind cluster stopped"
else
    echo "  - Kind cluster not running"
fi

echo
echo "=========================================="
echo "  Development Environment Stopped"
echo "=========================================="
echo
echo "All services, client, proxy, and cluster stopped."
echo "Data and volumes are retained."
echo
echo "To start again:"
echo "  ./scripts/dev-start.sh"
echo
echo "To completely reset (delete all data):"
echo "  ./scripts/dev-reset.sh"
echo
