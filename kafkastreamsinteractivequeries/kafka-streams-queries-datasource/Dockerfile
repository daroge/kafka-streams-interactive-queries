FROM openjdk:11-jre-slim
EXPOSE 7890
ARG JAR_FILE
ADD ${JAR_FILE} /usr/src/apps/data.jar
WORKDIR /usr/src/apps
ENTRYPOINT ["java","-jar","data.jar"]