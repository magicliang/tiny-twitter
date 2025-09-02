#!/bin/bash

# Twitter Clone Deployment Script
# This script builds and deploys the Twitter Clone application to Kubernetes

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="twitter-clone"
IMAGE_TAG="latest"
NAMESPACE="twitter-clone"

echo -e "${GREEN}🚀 Starting Twitter Clone Deployment${NC}"

# Function to print status
print_status() {
    echo -e "${YELLOW}📋 $1${NC}"
}

# Function to print success
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Function to print error
print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    print_error "kubectl is not installed. Please install kubectl and try again."
    exit 1
fi

# Check if Kubernetes cluster is accessible
if ! kubectl cluster-info > /dev/null 2>&1; then
    print_error "Cannot connect to Kubernetes cluster. Please check your kubeconfig."
    exit 1
fi

# Build the application
print_status "Building the application..."
./mvnw clean package -DskipTests
print_success "Application built successfully"

# Build Docker image
print_status "Building Docker image..."
docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
print_success "Docker image built successfully"

# Create namespace if it doesn't exist
print_status "Creating namespace..."
kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
print_success "Namespace created/updated"

# Apply Kubernetes resources
print_status "Deploying to Kubernetes..."
kubectl apply -k k8s/
print_success "Kubernetes resources applied"

# Wait for deployment to be ready
print_status "Waiting for deployment to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/twitter-clone-app -n ${NAMESPACE}
print_success "Deployment is ready"

# Get service information
print_status "Getting service information..."
echo ""
echo "📊 Deployment Status:"
kubectl get pods -n ${NAMESPACE}
echo ""
kubectl get services -n ${NAMESPACE}
echo ""

# Get NodePort information
NODEPORT=$(kubectl get service twitter-clone-nodeport -n ${NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}')
if [ ! -z "$NODEPORT" ]; then
    echo "🌐 Application is accessible at:"
    echo "   NodePort: http://localhost:${NODEPORT}/api"
    echo "   Health Check: http://localhost:${NODEPORT}/api/actuator/health"
    echo "   H2 Console: http://localhost:${NODEPORT}/api/h2-console"
fi

# Check if ingress is configured
INGRESS=$(kubectl get ingress twitter-clone-ingress -n ${NAMESPACE} --ignore-not-found)
if [ ! -z "$INGRESS" ]; then
    echo "   Ingress: http://twitter-clone.local/api"
fi

echo ""
print_success "🎉 Twitter Clone deployed successfully!"

echo ""
echo "📝 Useful commands:"
echo "   View logs: kubectl logs -f deployment/twitter-clone-app -n ${NAMESPACE}"
echo "   Scale app: kubectl scale deployment twitter-clone-app --replicas=5 -n ${NAMESPACE}"
echo "   Delete app: kubectl delete -k k8s/"
echo "   Port forward: kubectl port-forward service/twitter-clone-service 8080:80 -n ${NAMESPACE}"