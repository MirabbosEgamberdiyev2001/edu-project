#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# EduPlatform — One-time Server Setup Script
# Run this ONCE on the server before the first GitLab CI deployment.
#
# Usage:
#   ssh root@62.84.186.113
#   bash <(curl -s https://raw.githubusercontent.com/...) # or copy-paste
#
# What this script does:
#   1. Installs Docker + Docker Compose plugin
#   2. Creates /opt/eduplatform directory
#   3. Generates an SSH key pair for GitLab CI deployments
#   4. Prints the private key to copy into GitLab CI/CD Variables
# ═══════════════════════════════════════════════════════════════════════════════
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════════${NC}"
echo -e "${BLUE}  EduPlatform Server Setup                     ${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════${NC}"

# ── 1. System update ──────────────────────────────────────────────────────────
echo -e "\n${YELLOW}[1/4] Updating system packages...${NC}"
apt-get update -q
apt-get install -y -q ca-certificates curl gnupg lsb-release

# ── 2. Install Docker ─────────────────────────────────────────────────────────
echo -e "\n${YELLOW}[2/4] Installing Docker...${NC}"

if command -v docker &> /dev/null; then
    echo -e "${GREEN}  Docker already installed: $(docker --version)${NC}"
else
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
        | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg

    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
        | tee /etc/apt/sources.list.d/docker.list > /dev/null

    apt-get update -q
    apt-get install -y -q docker-ce docker-ce-cli containerd.io \
        docker-buildx-plugin docker-compose-plugin

    systemctl enable docker
    systemctl start docker
    echo -e "${GREEN}  Docker installed: $(docker --version)${NC}"
fi

# ── 3. Create app directory ───────────────────────────────────────────────────
echo -e "\n${YELLOW}[3/4] Creating app directory...${NC}"
mkdir -p /opt/eduplatform
echo -e "${GREEN}  Directory created: /opt/eduplatform${NC}"

# ── 4. Generate SSH deploy key ────────────────────────────────────────────────
echo -e "\n${YELLOW}[4/4] Generating SSH deploy key for GitLab CI...${NC}"

KEY_FILE="/root/.ssh/gitlab_deploy_key"

if [ -f "$KEY_FILE" ]; then
    echo -e "${YELLOW}  SSH key already exists at $KEY_FILE — skipping generation.${NC}"
else
    ssh-keygen -t ed25519 -C "gitlab-ci-eduplatform" -f "$KEY_FILE" -N ""
    echo -e "${GREEN}  SSH key pair generated.${NC}"
fi

# Add public key to authorized_keys
if ! grep -q "$(cat ${KEY_FILE}.pub)" /root/.ssh/authorized_keys 2>/dev/null; then
    cat "${KEY_FILE}.pub" >> /root/.ssh/authorized_keys
    chmod 600 /root/.ssh/authorized_keys
    echo -e "${GREEN}  Public key added to authorized_keys.${NC}"
else
    echo -e "${YELLOW}  Public key already in authorized_keys.${NC}"
fi

# ── Print instructions ────────────────────────────────────────────────────────
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ Server setup complete!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}NEXT STEP: Add this PRIVATE KEY to GitLab CI/CD Variables${NC}"
echo -e "  GitLab → Your Project → Settings → CI/CD → Variables"
echo -e "  Variable name: ${GREEN}SSH_PRIVATE_KEY${NC}"
echo -e "  Type: ${GREEN}File${NC} (important!)"
echo -e "  Protected: ${GREEN}Yes${NC}"
echo ""
echo -e "${RED}━━━━━━━━━━ PRIVATE KEY (copy everything below) ━━━━━━━━━━${NC}"
cat "$KEY_FILE"
echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}Also add these GitLab CI/CD Variables:${NC}"
echo -e "  SERVER_IP           = $(hostname -I | awk '{print $1}')"
echo -e "  DB_PASSWORD         = (choose a strong password)"
echo -e "  JWT_SECRET          = (random string, min 32 chars)"
echo -e "  FRONTEND_BASE_URL   = http://$(hostname -I | awk '{print $1}')"
echo -e "  CORS_ORIGINS        = http://$(hostname -I | awk '{print $1}')"
echo ""
echo -e "${GREEN}After setting variables, push to the 'main' branch to trigger deployment.${NC}"
echo ""
