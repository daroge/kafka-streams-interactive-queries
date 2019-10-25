ARG JAR_FILE
FROM openjdk:11-jre-slim
ADD ${JAR_FILE} /usr/src/apps/dataSource-1.0.jar
WORKDIR /usr/src/apps
ENTRYPOINT ["java","-jar","dataSource-1.0.jar"]