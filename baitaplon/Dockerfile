# ============================================================
# SLMS - Smart Library Management System
# Multi-stage Docker build (Java 17 + Maven)
# ============================================================

# ----- Stage 1: Build fat JAR -----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ----- Stage 2: Runtime -----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Thư mục H2 DB (mount volume nếu muốn giữ dữ liệu)
RUN mkdir -p /app/database

# Fat JAR từ stage build
COPY --from=build /build/target/slms-1.0-all.jar /app/slms.jar

# Frontend tĩnh (StaticFileHandler tìm src/main/webapp hoặc webapp)
COPY src/main/webapp /app/src/main/webapp
COPY src/main/webapp /app/webapp

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"

CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/slms.jar"]
