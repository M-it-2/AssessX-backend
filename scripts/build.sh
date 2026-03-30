#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

if ! command -v java &> /dev/null; then
    echo "Java not found. Installing..."
    sudo pacman -S --noconfirm jdk21-openjdk
fi

JAVA_PATH=$(dirname $(dirname $(readlink -f $(which java))))
echo "Java found: $JAVA_PATH"

if [ -n "$ZSH_VERSION" ]; then
    SHELL_RC="$HOME/.zshrc"
elif [ -n "$BASH_VERSION" ]; then
    SHELL_RC="$HOME/.bashrc"
else
    SHELL_RC="$HOME/.profile"
fi

echo "Using shell config: $SHELL_RC"

if ! grep -q "JAVA_HOME" "$SHELL_RC"; then
    echo "export JAVA_HOME=$JAVA_PATH" >> "$SHELL_RC"
    echo "JAVA_HOME added to $SHELL_RC"
else
    echo "JAVA_HOME already exists in $SHELL_RC"
fi

export JAVA_HOME=$JAVA_PATH

if ! command -v docker &> /dev/null; then
    echo "Docker not found. Please install it manually: sudo pacman -S docker"
    exit 1
fi

if ! docker compose version &> /dev/null; then
    echo "Docker Compose not found. Please install it: sudo pacman -S docker-compose"
    exit 1
fi

if ! groups $USER | grep -q docker; then
    echo "Adding $USER to docker group..."
    sudo usermod -aG docker $USER
fi

DOCKER_CMD="docker"
if ! docker info &> /dev/null 2>&1; then
    echo "Docker group not active in current session, using sudo..."
    DOCKER_CMD="sudo docker"
fi

echo "Building..."
cd "$ROOT_DIR/AssessX-backend"
./mvnw clean package -DskipTests

echo "Starting docker compose..."
cd "$ROOT_DIR"

# dev
# $DOCKER_CMD compose -f docker-compose.dev.yml up -d 

# prod
$DOCKER_CMD compose -f docker-compose.yml up -d 