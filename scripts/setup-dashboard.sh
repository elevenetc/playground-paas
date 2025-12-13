#!/bin/bash

set -e

echo "Setting up Kubernetes Dashboard..."

# Install Kubernetes Dashboard
echo "Installing Dashboard..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml

# Create admin service account
echo "Creating admin service account..."
kubectl create serviceaccount dashboard-admin-sa -n kubernetes-dashboard --dry-run=client -o yaml | kubectl apply -f -

# Create cluster role binding
kubectl create clusterrolebinding dashboard-admin-sa \
  --clusterrole=cluster-admin \
  --serviceaccount=kubernetes-dashboard:dashboard-admin-sa \
  --dry-run=client -o yaml | kubectl apply -f -

# Wait for dashboard to be ready
echo "Waiting for dashboard to be ready..."
kubectl wait --for=condition=available --timeout=60s deployment/kubernetes-dashboard -n kubernetes-dashboard

echo
echo "=========================================="
echo "  Kubernetes Dashboard Setup Complete!"
echo "=========================================="
echo
echo "To access the dashboard:"
echo
echo "1. Start the proxy (in a separate terminal):"
echo "   kubectl proxy"
echo
echo "2. Open in your browser:"
echo "   http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/"
echo
echo "3. Get the access token:"
echo "   ./scripts/get-dashboard-token.sh"
echo
