FROM openjdk:11-jre-slim
ARG JAR_FILE
EXPOSE 7070
ADD ${JAR_FILE} /usr/src/apps/processing-1.0.0.jar
WORKDIR /usr/src/apps
ENTRYPOINT ["java","-jar","processing-1.0.0.jar"]