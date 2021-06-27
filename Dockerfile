FROM maven:3.6.0-jdk-8-slim AS build

WORKDIR /

COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

FROM openjdk:8-jre-slim
COPY --from=build /home/app/target/backend.jar /usr/local/lib/backend.jar

EXPOSE 1337
ENTRYPOINT ["java","-Xmx32M","-jar","/usr/local/lib/backend.jar", "start"]
