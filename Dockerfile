# ==============================================================================
# Multi-Stage Dockerfile for BuyIt Multi-Vendor E-Commerce Platform
# Stage 1: Build React Production Bundle
# Stage 2: Compile Java Backend and Package Executable JAR
# Stage 3: Lightweight Runtime Container (Alpine OpenJDK JRE)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Frontend Builder
# ------------------------------------------------------------------------------
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci || npm install

COPY frontend/ ./
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 2: Backend Builder
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS backend-builder
WORKDIR /app

RUN apk add --no-cache curl

# Copy backend sources
COPY backend/ ./backend/

# Ensure PostgreSQL JDBC driver is present
RUN mkdir -p backend/lib backend/resources && \
    if [ ! -f "backend/lib/postgresql-42.7.4.jar" ]; then \
        curl -fsSL -o backend/lib/postgresql-42.7.4.jar \
            https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar; \
    fi

# Ensure default database.properties exists if ignored in git
RUN if [ ! -f "backend/resources/database.properties" ]; then \
        echo "db.url=jdbc:postgresql://db.wcoivrmtfvlcpwerhjwn.supabase.co:5432/postgres?sslmode=require&connectTimeout=10&socketTimeout=30" > backend/resources/database.properties && \
        echo "db.user=postgres" >> backend/resources/database.properties && \
        echo "db.password=Shyam@2007ronaldo" >> backend/resources/database.properties; \
    fi

# Compile Java backend
RUN mkdir -p out && \
    javac -cp "backend/lib/postgresql-42.7.4.jar" -d out \
        backend/*.java \
        backend/model/*.java \
        backend/service/*.java \
        backend/db/*.java \
        backend/util/*.java

# Bundle resources & package executable buyit.jar
RUN mkdir -p out/resources out/lib && \
    cp -r backend/resources/* out/resources/ && \
    cp backend/resources/database.properties out/database.properties && \
    cp backend/lib/*.jar out/lib/ && \
    echo "Main-Class: Main" > out/manifest.txt && \
    echo "Class-Path: lib/postgresql-42.7.4.jar resources/" >> out/manifest.txt && \
    jar cfm out/buyit.jar out/manifest.txt -C out . && \
    rm -f out/manifest.txt

# ------------------------------------------------------------------------------
# Stage 3: Minimal Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Install curl for cloud container health checks
RUN apk add --no-cache curl

# Copy compiled backend and libraries
COPY --from=backend-builder /app/out/buyit.jar ./buyit.jar
COPY --from=backend-builder /app/out/lib ./lib
COPY --from=backend-builder /app/out/resources ./resources
COPY --from=backend-builder /app/out/database.properties ./database.properties

# Copy compiled React SPA
COPY --from=frontend-builder /app/frontend/dist ./frontend/dist

# Copy 1,000 product images catalog (~29 MB)
COPY amazon-capstone/product-images ./amazon-capstone/product-images

# Default environment configuration
ENV PORT=8080
ENV JAVA_OPTS="-Djava.awt.headless=true -XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

# Expose HTTP port
EXPOSE 8080

# Cloud Health Check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:${PORT}/api/health || exit 1

# Start BuyIt Full-Stack Server
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar buyit.jar"]
