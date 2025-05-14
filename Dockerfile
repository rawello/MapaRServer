FROM gradle:8.12.0-jdk21 AS builder

WORKDIR /home/gradle/project

COPY . .

RUN gradle clean build --no-daemon

FROM ghcr.io/graalvm/graalvm-community:23.0.1

WORKDIR /app

COPY --from=builder /home/gradle/project/build/libs/mapar_backend.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]