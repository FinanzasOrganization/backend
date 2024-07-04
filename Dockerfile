FROM eclipse-temurin:17-jdk
ARG JAR_FILE=target/gofinance-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} gofinance.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","gofinance.jar"]
