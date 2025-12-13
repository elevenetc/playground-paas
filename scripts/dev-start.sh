#!/bin/bash

set -e

echo "Starting development environment..."
echo

# Create logs directory if it doesn't exist
mkdir -p logs

# Ensure Kind cluster is running
./scripts/ensure-kind.sh

echo
echo "Starting docker-compose services..."
docker-compose up -d

echo
echo "Starting client..."
cd client

# Check if node_modules exists, if not run npm install
if [ ! -d "node_modules" ]; then
    echo "Installing client dependencies..."
    npm install > /dev/null 2>&1
fi

# Start client dev server in background
npm run dev > ../logs/client.log 2>&1 &
CLIENT_PID=$!
echo "Client started (PID: $CLIENT_PID)"

# Wait for client to be ready
echo "Waiting for client to be ready..."
for _ in {1..30}; do
    if curl -s http://localhost:5173 > /dev/null 2>&1; then
        break
    fi
    sleep 1
done

cd ..

echo
echo "Setting up Kubernetes Dashboard..."

# Check if dashboard is already installed
if ! kubectl get deployment kubernetes-dashboard -n kubernetes-dashboard &> /dev/null; then
    echo "Installing dashboard..."
    kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml > /dev/null 2>&1

    # Create admin service account
    kubectl create serviceaccount dashboard-admin-sa -n kubernetes-dashboard --dry-run=client -o yaml | kubectl apply -f - > /dev/null 2>&1
    kubectl create clusterrolebinding dashboard-admin-sa \
      --clusterrole=cluster-admin \
      --serviceaccount=kubernetes-dashboard:dashboard-admin-sa \
      --dry-run=client -o yaml | kubectl apply -f - > /dev/null 2>&1

    echo "Waiting for dashboard to be ready..."
    kubectl wait --for=condition=available --timeout=60s deployment/kubernetes-dashboard -n kubernetes-dashboard > /dev/null 2>&1
else
    echo "Dashboard already installed"
fi

# Start kubectl proxy if not already running
if ! lsof -Pi :8001 -sTCP:LISTEN -t &> /dev/null; then
    echo "Starting kubectl proxy..."
    kubectl proxy &> /dev/null &
    sleep 2
else
    echo "kubectl proxy already running"
fi

# Generate token
DASHBOARD_TOKEN=$(kubectl create token dashboard-admin-sa -n kubernetes-dashboard --duration=24h 2>/dev/null)

echo
echo "=========================================="
echo "  Development Environment Ready!"
echo "=========================================="
echo
echo "Services:"
echo "  - PostgreSQL: localhost:5432"
echo "  - Redis: localhost:6379"
echo "  - Kubernetes: kubectl cluster-info --context kind-paas-cluster"
echo "  - Client (React): http://localhost:5173"
echo
echo "Kubernetes Dashboard:"
echo "  - URL: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/"
echo "  - Token (valid 24h):"
echo "    $DASHBOARD_TOKEN"
echo
echo "Logs:"
echo "  - Client: tail -f logs/client.log"
echo "  - Docker services: docker-compose logs -f"
echo
echo "Useful commands:"
echo "  - Stop everything (keep data): ./scripts/dev-down.sh"
echo "  - Stop only services (keep data): ./scripts/dev-services-down.sh"
echo "  - Reset everything (delete data): ./scripts/dev-reset.sh"
echo "  - Get new dashboard token: ./scripts/get-dashboard-token.sh"
echo "  - View client logs: tail -f logs/client.log"
echo
