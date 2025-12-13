#!/bin/bash

set -e

echo "=========================================="
echo "  Development Environment Reset"
echo "=========================================="
echo
echo "Resetting:"
echo "  - Docker Compose services and volumes"
echo "  - Kind cluster and all deployments"
echo "  - Build artifacts"
echo
echo "Step 1: Stopping all services..."
echo "  - Stopping client..."
pkill -f "vite" 2>/dev/null && echo "    ✓ Client stopped" || echo "    - Client not running"
echo "  - Stopping kubectl proxy..."
lsof -ti :8001 | xargs kill -9 2>/dev/null && echo "    ✓ kubectl proxy stopped" || echo "    - kubectl proxy not running"
echo "  - Stopping Docker Compose services..."
docker-compose down -v || echo "    - No docker-compose services to stop"

echo
echo "Step 2: Deleting Docker Compose volumes..."
docker volume rm playground-paas_postgres-data 2>/dev/null && echo "  ✓ Deleted postgres-data volume" || echo "  - postgres-data volume doesn't exist"
docker volume rm playground-paas_redis-data 2>/dev/null && echo "  ✓ Deleted redis-data volume" || echo "  - redis-data volume doesn't exist"

echo
echo "Step 3: Deleting Kind cluster..."
if kind get clusters 2>/dev/null | grep -q "^paas-cluster$"; then
    kind delete cluster --name paas-cluster
    echo "  ✓ Kind cluster deleted"
else
    echo "  - Kind cluster doesn't exist"
fi

echo
echo "Step 4: Cleaning build artifacts..."
if [ -d "foundation/build/functions" ]; then
    rm -rf foundation/build/functions
    echo "  ✓ Deleted function build artifacts"
else
    echo "  - No function build artifacts to delete"
fi

echo
echo "Step 5: Removing Docker images for functions..."
docker images --format "{{.Repository}}:{{.Tag}}" | grep "^playground-paas-function-" | xargs -r docker rmi -f 2>/dev/null && echo "  ✓ Deleted function images" || echo "  - No function images to delete"

echo
echo "Step 6: Cleaning logs..."
if [ -d "logs" ]; then
    rm -rf logs
    echo "  ✓ Deleted logs directory"
else
    echo "  - No logs to delete"
fi

echo
echo "=========================================="
echo "  Reset Complete!"
echo "=========================================="
echo
echo "To start fresh, run:"
echo "  ./scripts/start-dev.sh"
echo
