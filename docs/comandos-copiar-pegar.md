# Comandos Copiar/Pegar Listos

## APACHE (44.222.107.0)

Conecta por SSH, luego copia y ejecuta bloques completos:

### Bloque 1: Actualizar e instalar certbot

```bash
sudo dnf update -y && sudo dnf install -y certbot python3-certbot-apache
```

### Bloque 2: Emitir certificado Let's Encrypt

```bash
sudo certbot certonly --apache \
  -d ec2-44-222-107-0.compute-1.amazonaws.com \
  --non-interactive \
  --agree-tos \
  -m tu-email@ejemplo.com
```

**⚠️ Cambia `tu-email@ejemplo.com` por tu email real**

### Bloque 3: Descargar cliente desde tu máquina local y copiar a Apache

En tu máquina (NO en el servidor):

```bash
scp -i tu-clave.pem -r apache-client/* ec2-user@44.222.107.0:/tmp/
```

Luego en el servidor Apache (vuelve a SSH):

```bash
sudo rm -rf /var/www/html/* && \
sudo mv /tmp/index.html /tmp/app.js /tmp/styles.css /var/www/html/ && \
sudo chown -R apache:apache /var/www/html/
```

### Bloque 4: Verificar

```bash
sudo systemctl restart httpd && \
sudo systemctl status httpd && \
curl https://ec2-44-222-107-0.compute-1.amazonaws.com/index.html -k
```

---

## SPRING (54.146.31.33)

Conecta por SSH, luego:

### Bloque 1: Instalar certbot

```bash
sudo dnf update -y && sudo dnf install -y certbot
```

### Bloque 2: Emitir certificado Let's Encrypt

```bash
sudo certbot certonly --standalone \
  -d ec2-54-146-31-33.compute-1.amazonaws.com \
  --non-interactive \
  --agree-tos \
  -m tu-email@ejemplo.com
```

**⚠️ Cambia `tu-email@ejemplo.com` por tu email real**

### Bloque 3: Convertir certificado a PKCS12

```bash
sudo mkdir -p /opt/certs && \
sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/ec2-54-146-31-33.compute-1.amazonaws.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/ec2-54-146-31-33.compute-1.amazonaws.com/privkey.pem \
  -out /opt/certs/spring.p12 \
  -name spring \
  -passout pass:ChangeMePassword123 && \
sudo chmod 644 /opt/certs/spring.p12 && \
sudo chown ec2-user:ec2-user /opt/certs/spring.p12
```

### Bloque 4: Crear archivo de configuración

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

### Bloque 5: Descargar JAR desde tu máquina y copiar a Spring

En tu máquina (NO en el servidor):

```bash
cd spring-backend && \
mvn clean package -DskipTests && \
scp -i tu-clave.pem target/secure-app-1.0.0.jar ec2-user@54.146.31.33:/tmp/
```

Luego en el servidor Spring (vuelve a SSH):

```bash
java -jar /tmp/secure-app-1.0.0.jar \
  --spring.config.location=file:/home/ec2-user/application.properties
```

Spring debe iniciar y mostrar logs en HTTPS. Espera a ver:

```
Started SecureAppApplication in X seconds
```

---

## PRUEBAS (en tu máquina local)

Abre una nueva terminal (los servidores siguen corriendo en sus SSH):

### Test 1: Acceder al cliente Apache

```bash
curl https://ec2-44-222-107-0.compute-1.amazonaws.com -k | head -20
```

Deberías ver HTML.

### Test 2: Registrar usuario en Spring

```bash
curl -k https://ec2-54-146-31-33.compute-1.amazonaws.com:8443/api/auth/register \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"TestPass123"}'
```

Respuesta esperada:

```json
{"message":"Usuario registrado correctamente"}
```

### Test 3: Login

```bash
curl -k https://ec2-54-146-31-33.compute-1.amazonaws.com:8443/api/auth/login \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"TestPass123"}'
```

Respuesta esperada:

```json
{"message":"Login exitoso","token":"<uuid-aqui>"}
```

Copia el token (sin comillas).

### Test 4: Endpoint protegido

```bash
curl -k https://ec2-54-146-31-33.compute-1.amazonaws.com:8443/api/secure/hello \
  -H "Authorization: Bearer <PEGA-TU-TOKEN-AQUI>"
```

Respuesta esperada:

```json
{"message":"Conexion segura y autenticada","user":"testuser"}
```

---

## NAVEGADOR (para capturas)

Abre en navegador:

```
https://ec2-44-222-107-0.compute-1.amazonaws.com
```

El navegador alertará de certificado no verificado (normal con Let's Encrypt en dominios AWS). Continúa.

Deberías ver:
- Formulario de registro
- Formulario de login
- Botón "Probar /api/secure/hello"
- Pantalla de salida abajo

Prueba:
1. Registrar usuario
2. Login
3. Llamar endpoint protegido

Captura screenshots en cada paso.

---

## NOTAS

- `tu-email@ejemplo.com` → usa tu email real, Let's Encrypt envía avisos de renovación
- `tu-clave.pem` → sustituye por tu archivo .pem real
- `ChangeMePassword123` → puedes cambiar, pero luego reemplaza en application.properties
- Si un comando falla, reporta el error exacto
- Los comandos están probados para Amazon Linux 2023
