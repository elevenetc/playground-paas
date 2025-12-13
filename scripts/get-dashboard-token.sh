#!/bin/bash

echo "Retrieving Kubernetes Dashboard access token..."
echo

# Get the token
TOKEN=$(kubectl create token dashboard-admin-sa -n kubernetes-dashboard --duration=24h)

echo "=========================================="
echo "  Dashboard Access Token"
echo "=========================================="
echo
echo "$TOKEN"
echo
echo "This token is valid for 24 hours."
echo
echo "To access the dashboard:"
echo "1. Run: kubectl proxy"
echo "2. Open: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/"
echo "3. Select 'Token' and paste the token above"
echo
