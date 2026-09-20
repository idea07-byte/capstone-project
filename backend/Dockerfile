# ==============================================================================
# Standalone Backend Dockerfile for Render / Cloud Deployment
# ==============================================================================

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

RUN apk add --no-cache curl

COPY . ./backend/

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
    cp -r backend/resources/* out/resources/ 2>/dev/null || true && \
    cp backend/resources/database.properties out/database.properties 2>/dev/null || true && \
    cp backend/lib/*.jar out/lib/ 2>/dev/null || true && \
    echo "Main-Class: Main" > out/manifest.txt && \
    echo "Class-Path: lib/postgresql-42.7.4.jar resources/" >> out/manifest.txt && \
    jar cfm out/buyit.jar out/manifest.txt -C out . && \
    rm -f out/manifest.txt

# ------------------------------------------------------------------------------
# Runtime Container
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /app/out/buyit.jar ./buyit.jar
COPY --from=builder /app/out/lib ./lib
COPY --from=builder /app/out/resources ./resources
COPY --from=builder /app/out/database.properties ./database.properties

ENV PORT=8080
ENV JAVA_OPTS="-Djava.awt.headless=true -XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:${PORT}/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar buyit.jar"]
