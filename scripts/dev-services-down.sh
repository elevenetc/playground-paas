#!/bin/bash

set -e

echo "=========================================="
echo "  Stopping Services (Data Retained)"
echo "=========================================="
echo

echo "Stopping Docker Compose services..."
docker-compose stop

echo
echo "=========================================="
echo "  Services Stopped Successfully"
echo "=========================================="
echo
echo "All services are stopped but data is retained."
echo
echo "To start again:"
echo "  ./scripts/dev-start.sh"
echo
echo "Note: Kind cluster, kubectl proxy, and client are still running"
echo "To stop everything including client: ./scripts/dev-down.sh"
echo
