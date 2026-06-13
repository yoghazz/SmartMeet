#!/bin/bash
# ============================================================
# SmartMeet Backend — Auto Installer for Ubuntu 20.04/22.04/24.04
# Usage: bash install.sh
# ============================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log()  { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[✗]${NC} $1"; exit 1; }
info() { echo -e "${BLUE}[i]${NC} $1"; }

echo ""
echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}   SmartMeet Backend Installer${NC}"
echo -e "${BLUE}=================================================${NC}"
echo ""

# ── 1. Check root ──────────────────────────────────────────
if [ "$EUID" -ne 0 ]; then
  err "Jalankan sebagai root: sudo bash install.sh"
fi

# ── 2. Update system ───────────────────────────────────────
info "Update system packages..."
apt-get update -qq && apt-get upgrade -y -qq
log "System updated"

# ── 3. Install dependencies ────────────────────────────────
info "Install dependencies..."
apt-get install -y -qq \
  curl wget git ca-certificates gnupg lsb-release \
  apt-transport-https software-properties-common \
  ufw fail2ban
log "Dependencies installed"

# ── 4. Install Docker ──────────────────────────────────────
if ! command -v docker &>/dev/null; then
  info "Install Docker..."
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] \
    https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -qq
  apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin
  systemctl enable docker
  systemctl start docker
  log "Docker installed"
else
  log "Docker already installed: $(docker --version)"
fi

# ── 5. Install Docker Compose standalone ──────────────────
if ! command -v docker-compose &>/dev/null; then
  info "Install Docker Compose..."
  COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep '"tag_name"' | cut -d'"' -f4)
  curl -SL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-x86_64" \
    -o /usr/local/bin/docker-compose
  chmod +x /usr/local/bin/docker-compose
  log "Docker Compose installed: $(docker-compose --version)"
else
  log "Docker Compose already installed"
fi

# ── 6. Clone or update repo ────────────────────────────────
INSTALL_DIR="/opt/smartmeet"

if [ -d "$INSTALL_DIR/.git" ]; then
  info "Repo already exists, pulling latest..."
  cd "$INSTALL_DIR"
  git pull origin main
  log "Repo updated"
else
  info "Clone SmartMeet repository..."
  echo ""
  read -rp "  Masukkan GitHub repo URL (contoh: https://github.com/user/smartmeet.git): " REPO_URL
  if [ -z "$REPO_URL" ]; then
    err "Repo URL tidak boleh kosong"
  fi
  git clone "$REPO_URL" "$INSTALL_DIR"
  cd "$INSTALL_DIR"
  log "Repo cloned to $INSTALL_DIR"
fi

cd "$INSTALL_DIR"

# ── 7. Setup .env ──────────────────────────────────────────
if [ ! -f "backend/.env" ]; then
  info "Setup environment variables..."
  cp backend/.env.example backend/.env

  echo ""
  echo -e "${YELLOW}=== Konfigurasi SmartMeet ===${NC}"
  echo ""

  # Generate random secrets
  SECRET_KEY=$(openssl rand -hex 32)
  JWT_SECRET=$(openssl rand -hex 32)

  read -rp "  OPENAI_API_KEY (sk-...): " OPENAI_KEY
  if [ -z "$OPENAI_KEY" ]; then
    warn "OPENAI_API_KEY kosong — set nanti di backend/.env"
    OPENAI_KEY="sk-ganti-dengan-api-key-openai"
  fi

  read -rp "  Whisper model [tiny/base/small/medium] (default: small): " WHISPER_MODEL
  WHISPER_MODEL=${WHISPER_MODEL:-small}

  read -rp "  OpenAI model [gpt-4o/gpt-4o-mini] (default: gpt-4o-mini): " OPENAI_MODEL
  OPENAI_MODEL=${OPENAI_MODEL:-gpt-4o-mini}

  # Write to .env
  sed -i "s|SECRET_KEY=.*|SECRET_KEY=$SECRET_KEY|g" backend/.env
  sed -i "s|JWT_SECRET_KEY=.*|JWT_SECRET_KEY=$JWT_SECRET|g" backend/.env
  sed -i "s|OPENAI_API_KEY=.*|OPENAI_API_KEY=$OPENAI_KEY|g" backend/.env
  sed -i "s|WHISPER_MODEL=.*|WHISPER_MODEL=$WHISPER_MODEL|g" backend/.env
  sed -i "s|OPENAI_MODEL=.*|OPENAI_MODEL=$OPENAI_MODEL|g" backend/.env
  sed -i "s|DEBUG=.*|DEBUG=false|g" backend/.env

  log ".env configured"
else
  warn "backend/.env already exists, skipping .env setup"
fi

# ── 8. Firewall ────────────────────────────────────────────
info "Configure firewall..."
ufw --force reset > /dev/null
ufw default deny incoming > /dev/null
ufw default allow outgoing > /dev/null
ufw allow ssh > /dev/null
ufw allow 8000/tcp comment 'SmartMeet API' > /dev/null
# Port lain (5432, 6379, 9000) TIDAK dibuka — internal Docker saja
ufw --force enable > /dev/null
log "Firewall configured (SSH + 8000 open)"

# ── 9. Start services ──────────────────────────────────────
info "Building and starting SmartMeet services..."
cd "$INSTALL_DIR"
docker-compose -f docker-compose.prod.yml up -d --build

# Wait for services
info "Waiting for services to be healthy..."
sleep 15

# ── 10. Run migrations ─────────────────────────────────────
info "Running database migrations..."
docker-compose -f docker-compose.prod.yml exec -T api alembic upgrade head
log "Migrations applied"

# ── 11. Health check ───────────────────────────────────────
info "Health check..."
sleep 3
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/health)
if [ "$HTTP_STATUS" = "200" ]; then
  log "API is healthy (HTTP 200)"
else
  warn "API health check returned HTTP $HTTP_STATUS — check logs: docker-compose -f docker-compose.prod.yml logs api"
fi

# ── Done ───────────────────────────────────────────────────
echo ""
echo -e "${GREEN}=================================================${NC}"
echo -e "${GREEN}   SmartMeet Backend berhasil diinstall!${NC}"
echo -e "${GREEN}=================================================${NC}"
echo ""
echo -e "  API URL  : ${BLUE}http://$(hostname -I | awk '{print $1}'):8000${NC}"
echo -e "  API Docs : ${BLUE}http://$(hostname -I | awk '{print $1}'):8000/docs${NC}"
echo ""
echo -e "  Setup IIS reverse proxy di Server A ke:"
echo -e "  ${YELLOW}http://$(hostname -I | awk '{print $1}'):8000${NC}"
echo ""
echo -e "  Useful commands:"
echo -e "  ${BLUE}docker-compose -f docker-compose.prod.yml logs -f api${NC}     # API logs"
echo -e "  ${BLUE}docker-compose -f docker-compose.prod.yml ps${NC}              # Status services"
echo -e "  ${BLUE}docker-compose -f docker-compose.prod.yml restart api${NC}     # Restart API"
echo -e "  ${BLUE}nano /opt/smartmeet/backend/.env${NC}                           # Edit config"
echo ""
