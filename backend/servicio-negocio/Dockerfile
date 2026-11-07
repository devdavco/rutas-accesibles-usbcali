# Imagen de servicio-negocio (datos del grafo, autenticación).
# Contexto: backend/  →  docker build -f backend/servicio-negocio/Dockerfile -t <imagen> backend
# Compatible con el constructor heredado y con BuildKit (sin --mount ni # syntax).

# --- 1. Compilación con Gradle (JDK 25) -------------------------------------------------
FROM eclipse-temurin:25-jdk AS compilacion
WORKDIR /fuente

# Primero solo la configuración de Gradle: esta capa (con las dependencias descargadas) se
# reutiliza mientras no cambien los build.gradle.kts ni el catálogo de versiones
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY servicio-negocio/build.gradle.kts servicio-negocio/
COPY servicio-grafos/build.gradle.kts servicio-grafos/
COPY gateway/build.gradle.kts gateway/
RUN ./gradlew :servicio-negocio:dependencies --no-daemon --quiet > /dev/null

# Luego el código. Las pruebas no corren aquí (Testcontainers necesita Docker): se ejecutan
# con ./gradlew build antes de construir las imágenes
COPY servicio-negocio/src servicio-negocio/src
RUN ./gradlew :servicio-negocio:bootJar -x test --no-daemon --quiet \
 && cp servicio-negocio/build/libs/servicio-negocio-*.jar /fuente/app.jar \
 && java -Djarmode=tools -jar /fuente/app.jar extract --layers --launcher --destination /fuente/capas

# --- 2. Ejecución con un JRE 25 liviano ---------------------------------------------------
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app

# Capas de la más estable a la que más cambia: al cambiar el código solo se reemplaza la última
COPY --from=compilacion --chown=app:app /fuente/capas/dependencies/ ./
COPY --from=compilacion --chown=app:app /fuente/capas/spring-boot-loader/ ./
COPY --from=compilacion --chown=app:app /fuente/capas/snapshot-dependencies/ ./
COPY --from=compilacion --chown=app:app /fuente/capas/application/ ./

USER app
EXPOSE 8081

# JVM para contenedores: el heap se calcula sobre el límite de memoria del contenedor.
# SerialGC: menos memoria propia que G1 en heaps pequeños y con 2 vCPU. Se puede cambiar sin
# reconstruir (variable JAVA_OPTS)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Xss512k" \
    SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8081/actuator/health | grep -q '"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
