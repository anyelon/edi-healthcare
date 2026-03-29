#!/usr/bin/env bash
set -euo pipefail

# Usage: MONGODB_URI='mongodb://...' ./scripts/update_mongodb_secret.sh [dev|prod]

ENV="${1:-dev}"

if [[ "$ENV" != "dev" && "$ENV" != "prod" ]]; then
  echo "Error: environment must be 'dev' or 'prod', got '$ENV'"
  exit 1
fi

if [[ "$ENV" == "dev" ]]; then
  PROJECT_ID="edi-healthcare-dev"
  SECRET_NAME="mongodb-uri-dev"
else
  PROJECT_ID="edi-healthcare-prod"
  SECRET_NAME="mongodb-uri-prod"
fi

if [[ -z "${MONGODB_URI:-}" ]]; then
  echo "ERROR: MONGODB_URI environment variable is not set."
  echo ""
  echo "Usage: MONGODB_URI='mongodb://...' ./scripts/update_mongodb_secret.sh $ENV"
  echo ""
  echo "Get the URI from: GCP Console > Firestore > Select your database > Connection string"
  exit 1
fi

echo "Updating secret '$SECRET_NAME' in project '$PROJECT_ID'..."
echo -n "$MONGODB_URI" | gcloud secrets versions add "$SECRET_NAME" --data-file=- --project="$PROJECT_ID"
echo "Done. Redeploy Cloud Run services to pick up the new secret version."
