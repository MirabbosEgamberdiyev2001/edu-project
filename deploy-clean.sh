#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# EduPlatform — Manual Clean Reset Script
# ═══════════════════════════════════════════════════════════════════════════════
# Run this on the SERVER when you want a full clean reset (wipes ALL data).
# Normal CI/CD deployments do NOT run this — they preserve the database.
#
# Usage (on server):
#   cd /opt/eduplatform
#   bash deploy-clean.sh
# ═══════════════════════════════════════════════════════════════════════════════
set -e

echo "⚠️  WARNING: This will DELETE all database data and file uploads!"
echo "   Press Ctrl+C within 5 seconds to cancel..."
sleep 5

cd /opt/eduplatform

echo "=== Stopping all containers and DELETING all volumes ==="
docker compose down --volumes --remove-orphans 2>/dev/null || true

echo "=== Pruning all unused Docker resources (including volumes) ==="
docker system prune -af --volumes 2>/dev/null || true

echo "=== Starting fresh from zero ==="
docker compose up -d

echo "=== Waiting for database to be ready ==="
for i in $(seq 1 30); do
  sleep 5
  if docker compose exec -T db pg_isready -U postgres -d eduplatform &>/dev/null; then
    echo "✅ Database is ready!"
    break
  fi
  echo "Waiting for DB... ($i/30)"
done

echo ""
echo "=== Final status ==="
docker compose ps
echo ""
echo "✅ Clean reset complete. All data has been wiped and fresh deployment is running."
