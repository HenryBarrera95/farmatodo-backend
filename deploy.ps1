# Deploy Farmatodo a Cloud Run
# Uso: .\deploy.ps1
#
# Requisitos: Docker Desktop, gcloud CLI, .env con las variables

$ErrorActionPreference = "Stop"

# 1. Asegurar que gcloud esté en PATH (para docker-credential-gcloud)
$gcloudPath = "C:\Program Files (x86)\Google\Cloud SDK\google-cloud-sdk\bin"
if (-not ($env:PATH -like "*$gcloudPath*")) {
    $env:PATH = "$gcloudPath;$env:PATH"
}

$IMAGE = "us-east1-docker.pkg.dev/farmatodo-be-test/farmatodo/farmatodo:latest"
$SERVICE = "farmatodo"
$REGION = "us-east1"

Write-Host "=== 1. Build imagen Docker ===" -ForegroundColor Cyan
docker build -t farmatodo:local .
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "`n=== 2. Etiquetar para Artifact Registry ===" -ForegroundColor Cyan
docker tag farmatodo:local $IMAGE
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "`n=== 3. Subir a Artifact Registry ===" -ForegroundColor Cyan
docker push $IMAGE
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "`n=== 4. Desplegar en Cloud Run ===" -ForegroundColor Cyan
& "$gcloudPath\gcloud.cmd" run deploy $SERVICE `
    --image $IMAGE `
    --project farmatodo-be-test `
    --region $REGION `
    --platform managed `
    --allow-unauthenticated `
    --env-vars-file .env
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "`n=== Deploy completado ===" -ForegroundColor Green
Write-Host "URL: https://farmatodo-39721102308.us-east1.run.app" -ForegroundColor Yellow
