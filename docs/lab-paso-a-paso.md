# Lab Paso a Paso (Tus Servidores)

## IPs de tus servidores

- **Apache:** `44.222.107.0` → DNS: `ec2-44-222-107-0.compute-1.amazonaws.com`
- **Spring:** `54.146.31.33` → DNS: `ec2-54-146-31-33.compute-1.amazonaws.com`

## Paso 1: Preparar el código en local

1. El backend Spring ya está compilado en `/spring-backend`
2. El cliente está en `/apache-client` con URLs actualizadas

Compila el JAR final:

```bash
cd spring-backend
mvn clean package -DskipTests
```

Resultado: `spring-backend/target/secure-app-1.0.0.jar`

## Paso 2: Desplegar en servidores reales

**Consulta la guía completa:** [deployment-aws-tls.md](deployment-aws-tls.md)

Esa guía contiene:
- Comandos copiar/pegar para Apache
- Comandos copiar/pegar para Spring
- Configuración TLS con Let's Encrypt en ambos
- Pruebas paso a paso
- Troubleshooting

## Paso 3: Evidencias para entrega

Después de seguir la guía de deployment:

1. **Captura de HTTPS en Apache:**
   - Abre `https://ec2-44-222-107-0.compute-1.amazonaws.com`
   - Toma screenshot del navegador mostrando el candado.

2. **Captura de HTTPS en Spring:**
   - Desde terminal: `curl -k https://ec2-54-146-31-33.compute-1.amazonaws.com:8443/api/auth`
   - O intenta login desde el cliente y captura respuesta exitosa.

3. **Captura de flujo completo:**
   - Screenshot del cliente (Apache) mostrando:
     - Registro exitoso
     - Login exitoso con token
     - Endpoint protegido respondiendo con usuario autenticado

4. **Link de GitHub:**
   - Sube todo a un repositorio público

5. **Video demo (30 seg - 1 min):**
   - Mostrar cliente cargando desde Apache HTTPS
   - Flujo de login y acceso protegido
   - Arquitectura de 2 servidores

## Notas

- Los certificados Let's Encrypt se renuevan automáticamente cada 60 días (ver deployment-aws-tls.md)
- Los DNS de AWS ya funcionan públicamente, no necesitas un dominio aparte
- Si algo falla, revisa la sección "TROUBLESHOOTING" en deployment-aws-tls.md
