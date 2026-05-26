# Build stage: compile the Spring Boot app inside Docker.
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# Runtime stage: Java 21 runtime plus Python AI/RPA environment.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update && \
    apt-get install -y python3 python3-pip python3-venv wget fonts-nanum && \
    rm -rf /var/lib/apt/lists/*

RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
ENV PYTHONIOENCODING=UTF-8
ENV PYTHONUTF8=1
ENV PYTHONUNBUFFERED=1

COPY python-ai/requirements.txt ./python-ai/
RUN pip install --no-cache-dir -r python-ai/requirements.txt

# Install the browser runtime used by the RPA crawlers.
RUN playwright install --with-deps chromium

COPY python-ai/ ./python-ai/
COPY --from=build /app/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE $PORT

ENV JAVA_OPTS="-Xms128m -Xmx256m"
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dserver.port=${PORT} -jar app.jar"]
