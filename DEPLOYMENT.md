# SmartMeet — Panduan Deploy (2 Server)

```
[Android App]
      ↓ HTTPS
[Server A: Windows + IIS]  ← sityreq.online, SSL
      ↓ reverse proxy
[Server B: Ubuntu + Docker]  ← SmartMeet Backend
```

---

## Server B — Ubuntu Backend

### Step 1: Upload atau clone kode

```bash
# SSH ke Server B
ssh user@IP_SERVER_B

# Clone repo
git clone https://github.com/USERNAME/smartmeet.git /opt/smartmeet
cd /opt/smartmeet
```

### Step 2: Jalankan installer otomatis

```bash
sudo bash install.sh
```

Script akan:
1. Install Docker + Docker Compose
2. Setup firewall (buka SSH + port 8000)
3. Minta input: OPENAI_API_KEY, Whisper model, OpenAI model
4. Build dan start semua containers
5. Run database migrations
6. Health check

### Step 3: Verifikasi

```bash
# Cek semua container jalan
docker-compose -f docker-compose.prod.yml ps

# Cek API
curl http://localhost:8000/health

# Catat IP server B
hostname -I
```

Output IP ini yang akan dipakai di IIS Server A.

---

## Server A — Windows IIS Reverse Proxy

### Prerequisites

Download dan install dari Microsoft:
1. **ARR (Application Request Routing) 3.0**
   → https://www.iis.net/downloads/microsoft/application-request-routing
2. **URL Rewrite 2.1**
   → https://www.iis.net/downloads/microsoft/url-rewrite

Setelah install, restart IIS Manager.

---

### Step 1: Aktifkan Proxy di ARR

1. Buka **IIS Manager**
2. Klik nama server di panel kiri (bukan site)
3. Dobel klik **Application Request Routing Cache**
4. Di panel kanan klik **Server Proxy Settings**
5. Centang ✅ **Enable proxy**
6. Klik **Apply**

---

### Step 2: Buat Website Baru untuk SmartMeet

1. Di IIS Manager, klik kanan **Sites** → **Add Website**
2. Isi:
   - Site name: `smartmeet-api`
   - Physical path: `C:\inetpub\smartmeet` (buat folder kosong ini)
   - Binding:
     - Type: `http`
     - IP address: `All Unassigned`
     - Port: `80`
     - Host name: `api.sityreq.online`
3. Klik **OK**

---

### Step 3: Tambah HTTPS Binding (SSL)

1. Klik site `smartmeet-api`
2. Di panel kanan klik **Bindings**
3. Klik **Add**
4. Isi:
   - Type: `https`
   - Port: `443`
   - Host name: `api.sityreq.online`
   - SSL certificate: pilih certificate yang sudah ada
5. Klik **OK**

---

### Step 4: Konfigurasi Reverse Proxy (URL Rewrite)

1. Klik site `smartmeet-api`
2. Dobel klik **URL Rewrite**
3. Klik **Add Rule(s)** → **Reverse Proxy**
4. Isi:
   - Inbound: `api.sityreq.online`
   - Server name: `IP_SERVER_B:8000`
     (contoh: `192.168.1.100:8000` atau `103.x.x.x:8000`)
5. Centang ✅ **Enable SSL Offloading**
6. Klik **OK**

Ini akan otomatis membuat `web.config` di folder site.

---

### Step 5: Aktifkan WebSocket

WebSocket dibutuhkan untuk fitur real-time streaming SmartMeet.

1. Buka **Server Manager** → **Add Roles and Features**
2. Web Server (IIS) → **Application Development** → centang ✅ **WebSocket Protocol**
3. Install

Atau via PowerShell (jalankan sebagai Administrator):
```powershell
Install-WindowsFeature Web-WebSockets
```

---

### Step 6: Edit web.config untuk WebSocket

Buka `C:\inetpub\smartmeet\web.config`, pastikan ada konfigurasi ini:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <system.webServer>
    <rewrite>
      <rules>
        <!-- WebSocket: real-time audio streaming -->
        <rule name="SmartMeet WebSocket" stopProcessing="true">
          <match url="^ws/(.*)" />
          <action type="Rewrite" url="http://IP_SERVER_B:8000/ws/{R:1}" />
        </rule>
        <!-- HTTP API: semua request lain -->
        <rule name="SmartMeet API" stopProcessing="true">
          <match url="(.*)" />
          <action type="Rewrite" url="http://IP_SERVER_B:8000/{R:1}" />
        </rule>
      </rules>
    </rewrite>
    <webSocket enabled="true" />
    <security>
      <requestFiltering>
        <!-- Izinkan upload audio besar (500MB) -->
        <requestLimits maxAllowedContentLength="524288000" />
      </requestFiltering>
    </security>
  </system.webServer>
</configuration>
```

Ganti `IP_SERVER_B` dengan IP aktual Server B.

---

### Step 7: Update CORS di Server B

Edit `backend/.env` di Server B:

```bash
nano /opt/smartmeet/backend/.env
```

Tambahkan/ubah:
```env
ALLOWED_ORIGINS=https://api.sityreq.online,https://sityreq.online
```

Restart API:
```bash
cd /opt/smartmeet
docker-compose -f docker-compose.prod.yml restart api
```

---

### Step 8: Test

```bash
# Dari mana saja
curl https://api.sityreq.online/health

# Harusnya dapat:
# {"status":"ok","app":"SmartMeet","version":"1.0.0"}
```

Buka browser: `https://api.sityreq.online/docs` → Swagger UI SmartMeet

---

## DNS Setup

Di provider domain (Cloudflare/Niagahoster/dll), tambahkan A record:

| Name | Type | Value | TTL |
|---|---|---|---|
| `api` | A | `IP_SERVER_A` | Auto |

> Arahkan ke IP **Server A** (Windows/IIS), bukan Server B.

---

## Update Kode

Di Server B:
```bash
cd /opt/smartmeet
git pull origin main
docker-compose -f docker-compose.prod.yml up -d --build
docker-compose -f docker-compose.prod.yml exec -T api alembic upgrade head
```

---

## Troubleshooting

### API tidak bisa diakses dari Server A

```bash
# Test dari Server A (PowerShell)
curl http://IP_SERVER_B:8000/health

# Jika timeout, cek firewall Server B
sudo ufw status
# Pastikan port 8000 allow
```

### Cek logs

```bash
# Di Server B
docker-compose -f docker-compose.prod.yml logs -f api
docker-compose -f docker-compose.prod.yml logs -f worker-asr
```

### WebSocket tidak jalan

Pastikan di IIS:
- WebSocket Protocol sudah diinstall
- `<webSocket enabled="true" />` ada di web.config
- ARR Server Proxy Settings: **WebSocket** tidak di-disable

---

## Keamanan Tambahan (Opsional)

Di Server B, batasi port 8000 hanya untuk IP Server A:

```bash
# Hapus rule 8000 yang terbuka umum
sudo ufw delete allow 8000/tcp

# Buka hanya untuk IP Server A
sudo ufw allow from IP_SERVER_A to any port 8000
sudo ufw reload
```
