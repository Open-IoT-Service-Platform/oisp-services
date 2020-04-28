# Build the service
FROM maven:3.6-slim

ADD pom.xml /app/pom.xml
ADD src /app/src

WORKDIR /app

RUN mvn clean package -Pmy-flink-runner  -DskipTests