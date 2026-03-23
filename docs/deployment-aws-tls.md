# Deployment AWS con TLS - Step by Step

## IPs y Dominios

| Servidor | IP Pública | DNS Público | Puerto |
|----------|-----------|------------|--------|
| Apache | 44.222.107.0 | ec2-44-222-107-0.compute-1.amazonaws.com | 443 |
| Spring | 54.146.31.33 | ec2-54-146-31-33.compute-1.amazonaws.com | 8443 |

---

## SERVIDOR 1: APACHE

### Paso 1: Conectarse y actualizar

```bash
ssh -i tu-clave.pem ec2-user@44.222.107.0
sudo dnf update -y
```

### Paso 2: Instalar certbot y dependencias

```bash
sudo dnf install -y certbot python3-certbot-apache
```

### Paso 3: Emitir certificado Let's Encrypt para Apache

```bash
sudo certbot certonly --apache \
  -d ec2-44-222-107-0.compute-1.amazonaws.com \
  --non-interactive \
  --agree-tos \
  -m tu-email@example.com
```

**Nota:** Reemplaza `tu-email@example.com` con tu email real.

### Paso 4: Copiar archivos del cliente

En tu máquina local, desde la carpeta del proyecto:

```bash
scp -i tu-clave.pem -r apache-client/* ec2-user@44.222.107.0:/tmp/
```

Luego en el servidor Apache:

```bash
sudo rm -rf /var/www/html/*
sudo mv /tmp/index.html /tmp/app.js /tmp/styles.css /var/www/html/
sudo chown -R apache:apache /var/www/html/
```

### Paso 5: Verificar Apache y archivos

```bash
sudo systemctl status httpd
curl https://ec2-44-222-107-0.compute-1.amazonaws.com/index.html -k
```

Deberías ver el contenido HTML.

---

## SERVIDOR 2: SPRING

### Paso 1: Conectarse

```bash
ssh -i tu-clave.pem ec2-user@54.146.31.33
sudo dnf update -y
```

### Paso 2: Instalar certbot

```bash
sudo dnf install -y certbot
```

### Paso 3: Emitir certificado Let's Encrypt

Primero detén cualquier servicio en puerto 80 (si está activo):

```bash
sudo lsof -i :80
```

Si hay algo corriendo, detenlo. Luego:

```bash
sudo certbot certonly --standalone \
  -d ec2-54-146-31-33.compute-1.amazonaws.com \
  --non-interactive \
  --agree-tos \
  -m tu-email@example.com
```

### Paso 4: Convertir certificado a PKCS12

```bash
sudo mkdir -p /opt/certs
sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/ec2-54-146-31-33.compute-1.amazonaws.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/ec2-54-146-31-33.compute-1.amazonaws.com/privkey.pem \
  -out /opt/certs/spring.p12 \
  -name spring \
  -passout pass:ChangeMePassword123
  
sudo chmod 644 /opt/certs/spring.p12
sudo chown ec2-user:ec2-user /opt/certs/spring.p12
```

### Paso 5: Obtener el JAR actualizado

En tu máquina local (en la carpeta spring-backend):

```bash
mvn clean package -DskipTests
```

Luego envía el JAR a Spring:

```bash
scp -i tu-clave.pem spring-backend/target/secure-app-1.0.0.jar ec2-user@54.146.31.33:/tmp/
```

### Paso 6: Crear archivo de configuración en el servidor

En el servidor Spring, crea `/home/ec2-user/application.properties`:

```bash
cat > /home/ec2-user/application.properties << 'EOF'
server.port=8443

server.ssl.enabled=true
server.ssl.key-store=/opt/certs/spring.p12
server.ssl.key-store-password=ChangeMePassword123
server.ssl.key-store-type=PKCS12

spring.datasource.url=jdbc:h2:file:./data/securedb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.h2.console.enabled=false
EOF
```

### Paso 7: Ejecutar Spring Boot

```bash
java -jar /tmp/secure-app-1.0.0.jar \
  --spring.config.location=file:/home/ec2-user/application.properties
```

Deberías ver logs indicando que inicia en puerto 8443 HTTPS.

### Paso 8: Verificar conectividad

En otra terminal SSH del mismo servidor:

```bash
curl https://ec2-54-146-31-33.compute-1.amazonaws.com:8443/api/auth -k
```

La respuesta debe ser un error 405 (método no permitido), pero confirma que Spring responde en HTTPS.

---

## PRUEBAS FINALES

### 1. Acceder al cliente Apache

Abre en navegador:

```
https://ec2-44-222-107-0.compute-1.amazonaws.com
```

**Nota:** El navegador alertará de certificado autofirmado/no verificado en los primeros intentos. Esto es normal con Let's Encrypt en dominios de AWS. Continúa / Acepta.

### 2. Registrar usuario

En el formulario del cliente:
- Username: `testuser`
- Password: `TestPass123`
- Click: Registrar

Deberías ver: `{"message": "Usuario registrado correctamente"}`

### 3. Login

- Username: `testuser`
- Password: `TestPass123`
- Click: Ingresar

Deberías ver un token Bearer en la respuesta.

### 4. Endpoint protegido

Con el token del paso 3:
- Click: Probar /api/secure/hello

Deberías ver:
```json
{
  "message": "Conexion segura y autenticada",
  "user": "testuser"
}
```

---

## TROUBLESHOOTING

### Apache no sirve el cliente

```bash
sudo systemctl restart httpd
sudo tail -f /var/log/httpd/error_log
```

### Spring no inicia en HTTPS

1. Verifica que el certificado p12 existe:
   ```bash
   ls -la /opt/certs/spring.p12
   ```

2. Revisa logs de Spring en la terminal donde ejecutaste el jar.

3. Si dice "port already in use", mata el proceso anterior:
   ```bash
   lsof -i :8443
   kill -9 <PID>
   ```

### Certificado Let's Encrypt no emite

1. Verifica que el DNS resuelve:
   ```bash
   nslookup ec2-44-222-107-0.compute-1.amazonaws.com
   nslookup ec2-54-146-31-33.compute-1.amazonaws.com
   ```

2. Verifica puertos abiertos en Security Groups (80, 443 para certbot).

3. Si falla, prueba con `--debug` para ver detalles:
   ```bash
   sudo certbot certonly --apache --debug \
     -d ec2-44-222-107-0.compute-1.amazonaws.com
   ```

---

## Automatizar arranque de Spring (opcional pero recomendado)

Crea un servicio systemd para que Spring se levante automáticamente:

```bash
sudo tee /etc/systemd/system/spring-app.service > /dev/null << 'EOF'
[Unit]
Description=Spring Secure App
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/home/ec2-user
ExecStart=/usr/bin/java -jar /tmp/secure-app-1.0.0.jar \
  --spring.config.location=file:/home/ec2-user/application.properties
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable spring-app
sudo systemctl start spring-app
sudo systemctl status spring-app
```

Ahora Spring se reinicia automáticamente si cae.

---

## Renovación automática de certificados

Let's Encrypt emite certificados por 90 días. Para renovar automáticamente:

Apache:
```bash
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
```

Spring:
```bash
# Crear script de renovación
sudo tee /etc/letsencrypt/renewal-hooks/post-renew.sh > /dev/null << 'EOF'
#!/bin/bash
openssl pkcs12 -export \
  -in /etc/letsencrypt/live/ec2-54-146-31-33.compute-1.amazonaws.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/ec2-54-146-31-33.compute-1.amazonaws.com/privkey.pem \
  -out /opt/certs/spring.p12 \
  -name spring \
  -passout pass:ChangeMePassword123
systemctl restart spring-app
EOF

sudo chmod +x /etc/letsencrypt/renewal-hooks/post-renew.sh
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
```

Listo. Los certificados se renuevan solos cada 60 días antes de expirar.
