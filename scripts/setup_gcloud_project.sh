#!/usr/bin/env bash
set -euo pipefail

# Usage: ./setup_gcloud_project.sh [dev|prod]
# Default: dev

ENV="${1:-dev}"

if [[ "$ENV" != "dev" && "$ENV" != "prod" ]]; then
  echo "Error: environment must be 'dev' or 'prod', got '$ENV'"
  exit 1
fi

# === Environment-specific configuration ===
ORGANIZATION="anyelon"
REPO="edi-healthcare"
REGION="us-central1"
DATABASE_ID="(default)"
DEV_PROJECT_ID="edi-healthcare-dev"

if [[ "$ENV" == "dev" ]]; then
  PROJECT_ID="edi-healthcare-dev"
  SECRET_NAME="mongodb-uri-dev"
else
  PROJECT_ID="edi-healthcare-prod"
  SECRET_NAME="mongodb-uri-prod"
fi

echo ""
echo "============================================"
echo "  GCP Project Setup — $ENV environment"
echo "============================================"
echo "  Project:      $PROJECT_ID"
echo "  Region:       $REGION"
echo "  Organization: $ORGANIZATION"
echo "  Repository:   $REPO"
echo "============================================"
echo ""

# === Step 1: Set project and enable APIs ===
echo "=== Step 1: Setting active project and enabling APIs ==="
gcloud config set project "$PROJECT_ID"
echo "Active project set to $PROJECT_ID"

gcloud services enable \
    run.googleapis.com \
    artifactregistry.googleapis.com \
    secretmanager.googleapis.com \
    iamcredentials.googleapis.com \
    firestore.googleapis.com
echo "Required APIs enabled"

echo "Verifying enabled APIs..."
gcloud services list --enabled --filter="name:(run.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com iamcredentials.googleapis.com firestore.googleapis.com)"
echo ""

# === Step 2: Create Artifact Registry (dev only, shared across environments) ===
echo "=== Step 2: Creating Artifact Registry ==="
if [[ "$ENV" == "dev" ]]; then
  if gcloud artifacts repositories describe edi-healthcare --location="$REGION" --project="$PROJECT_ID" > /dev/null 2>&1; then
    echo "Artifact Registry 'edi-healthcare' already exists, skipping"
  else
    gcloud artifacts repositories create edi-healthcare \
        --repository-format=docker \
        --location="$REGION" \
        --project="$PROJECT_ID"
    echo "Artifact Registry 'edi-healthcare' created"
  fi
else
  echo "Skipping — Artifact Registry is shared from dev project ($DEV_PROJECT_ID)"
fi
echo ""

# === Step 3: Create Workload Identity Federation pool and provider ===
echo "=== Step 3: Setting up Workload Identity Federation ==="

if gcloud iam workload-identity-pools describe github-pool --location=global --project="$PROJECT_ID" > /dev/null 2>&1; then
  echo "Workload Identity Pool 'github-pool' already exists, skipping"
else
  gcloud iam workload-identity-pools create github-pool \
      --location=global \
      --project="$PROJECT_ID"
  echo "Workload Identity Pool 'github-pool' created"
fi

if gcloud iam workload-identity-pools providers describe github-provider --location=global --workload-identity-pool=github-pool --project="$PROJECT_ID" > /dev/null 2>&1; then
  echo "WIF provider 'github-provider' already exists, skipping"
else
  gcloud iam workload-identity-pools providers create-oidc github-provider \
      --location=global \
      --workload-identity-pool=github-pool \
      --issuer-uri=https://token.actions.githubusercontent.com \
      --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
      --attribute-condition="assertion.repository == \"$ORGANIZATION/$REPO\""
  echo "WIF provider 'github-provider' created"
fi
echo ""

# === Step 4: Create service account and assign IAM roles ===
echo "=== Step 4: Creating service account and assigning IAM roles ==="
SA_EMAIL="github-deployer@$PROJECT_ID.iam.gserviceaccount.com"

if gcloud iam service-accounts describe "$SA_EMAIL" --project="$PROJECT_ID" > /dev/null 2>&1; then
  echo "Service account 'github-deployer' already exists, skipping creation"
else
  gcloud iam service-accounts create github-deployer \
      --display-name="GitHub Actions Deployer" \
      --project="$PROJECT_ID"
  echo "Service account 'github-deployer' created"
fi

echo "Granting IAM roles to $SA_EMAIL..."

# Cloud Run Admin: deploy services
echo "  Granting roles/run.admin..."
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SA_EMAIL" \
    --role="roles/run.admin" \
    --condition=None \
    --quiet > /dev/null
echo "  roles/run.admin granted"

# Artifact Registry Writer: push Docker images (registry lives in dev project)
echo "  Granting roles/artifactregistry.writer on $DEV_PROJECT_ID..."
gcloud projects add-iam-policy-binding "$DEV_PROJECT_ID" \
    --member="serviceAccount:$SA_EMAIL" \
    --role="roles/artifactregistry.writer" \
    --condition=None \
    --quiet > /dev/null
echo "  roles/artifactregistry.writer granted on $DEV_PROJECT_ID"

# Secret Manager Accessor: read MongoDB URIs
echo "  Granting roles/secretmanager.secretAccessor..."
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SA_EMAIL" \
    --role="roles/secretmanager.secretAccessor" \
    --condition=None \
    --quiet > /dev/null
echo "  roles/secretmanager.secretAccessor granted"

# Service Account User: deploy to Cloud Run as that SA
echo "  Granting roles/iam.serviceAccountUser..."
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SA_EMAIL" \
    --role="roles/iam.serviceAccountUser" \
    --condition=None \
    --quiet > /dev/null
echo "  roles/iam.serviceAccountUser granted"

# Firestore/Datastore User: access Firestore MongoDB
echo "  Granting roles/datastore.user..."
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SA_EMAIL" \
    --role="roles/datastore.user" \
    --condition=None \
    --quiet > /dev/null
echo "  roles/datastore.user granted"

echo "All IAM roles assigned"
echo ""

# === Step 5: Bind Workload Identity to service account ===
echo "=== Step 5: Binding Workload Identity to service account ==="
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
WIF_MEMBER="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github-pool/attribute.repository/$ORGANIZATION/$REPO"

gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" \
    --project="$PROJECT_ID" \
    --role="roles/iam.workloadIdentityUser" \
    --member="$WIF_MEMBER" \
    --quiet > /dev/null
echo "Workload Identity bound: GitHub repo $ORGANIZATION/$REPO -> $SA_EMAIL"
echo ""

# === Step 6: Create Firestore MongoDB database ===
echo "=== Step 6: Creating Firestore MongoDB database ==="
if gcloud firestore databases describe --database="$DATABASE_ID" --project="$PROJECT_ID" > /dev/null 2>&1; then
  echo "Firestore database '$DATABASE_ID' already exists, skipping creation"
else
  gcloud firestore databases create \
      --project="$PROJECT_ID" \
      --location="$REGION" \
      --type=firestore-native \
      --database="$DATABASE_ID"
  echo "Firestore database '$DATABASE_ID' created"
fi
echo ""

# === Step 7: Store MongoDB URI in Secret Manager ===
echo "=== Step 7: Storing MongoDB URI in Secret Manager ==="
DATABASE_UID=$(gcloud firestore databases describe --database="$DATABASE_ID" --project="$PROJECT_ID" --format='value(uid)')
ENCODED_DB_ID=$(printf '%s' "$DATABASE_ID" | python3 -c "import sys, urllib.parse; print(urllib.parse.quote(sys.stdin.read()))")
MONGODB_URI="mongodb://${DATABASE_UID}.${REGION}.firestore.google.com:27017/${ENCODED_DB_ID}?authMechanism=MONGODB-AWS&authSource=%24external"
echo "Constructed MongoDB URI for database UID: $DATABASE_UID"

if gcloud secrets describe "$SECRET_NAME" --project="$PROJECT_ID" > /dev/null 2>&1; then
  echo "Secret '$SECRET_NAME' already exists, adding new version..."
else
  gcloud secrets create "$SECRET_NAME" \
      --replication-policy="automatic" \
      --project="$PROJECT_ID"
  echo "Secret '$SECRET_NAME' created"
fi

echo -n "$MONGODB_URI" | \
gcloud secrets versions add "$SECRET_NAME" --data-file=- --project="$PROJECT_ID"
echo "MongoDB URI stored as latest version of secret '$SECRET_NAME'"
echo ""

# === Summary ===
echo "============================================"
echo "  Setup complete for $ENV environment"
echo "============================================"
echo ""
echo "WIF_SERVICE_ACCOUNT: $SA_EMAIL"
echo ""
echo "WIF_PROVIDER:"
gcloud iam workload-identity-pools providers describe github-provider \
    --workload-identity-pool="github-pool" \
    --location="global" \
    --project="$PROJECT_ID" \
    --format='value(name)'
echo ""
echo "Set these as GitHub repository variables:"
echo "  GCP_PROJECT_$(echo "$ENV" | tr '[:lower:]' '[:upper:]')=$PROJECT_ID"
echo "  WIF_SERVICE_ACCOUNT=$SA_EMAIL"
echo "  WIF_PROVIDER=<value printed above>"
echo ""
echo "Done."
