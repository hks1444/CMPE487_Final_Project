FROM openjdk:17-slim

WORKDIR /app

RUN apt-get update && apt-get install -y wget
RUN wget https://repo1.maven.org/maven2/org/json/json/20230618/json-20230618.jar

COPY src /app/src

WORKDIR /app/src
RUN javac -cp ../json-20230618.jar exporter/*.java

EXPOSE 8080

CMD ["java", "-cp", "/app/src:/app/json-20230618.jar", "exporter.Main"]
