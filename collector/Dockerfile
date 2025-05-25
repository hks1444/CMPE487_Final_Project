FROM openjdk:17-slim

WORKDIR /app

RUN apt-get update && apt-get install -y wget
RUN wget https://repo1.maven.org/maven2/org/json/json/20230618/json-20230618.jar
RUN wget https://repo1.maven.org/maven2/org/java-websocket/Java-WebSocket/1.5.4/Java-WebSocket-1.5.4.jar
RUN wget https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar
RUN wget https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar

COPY src /app/src

WORKDIR /app
RUN javac -cp "json-20230618.jar:Java-WebSocket-1.5.4.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" src/collector/*.java

EXPOSE 4000/udp
EXPOSE 8080
EXPOSE 8081

CMD ["java", "--add-modules", "jdk.httpserver", "-cp", "/app/src:/app/json-20230618.jar:/app/Java-WebSocket-1.5.4.jar:/app/slf4j-api-2.0.9.jar:/app/slf4j-simple-2.0.9.jar", "collector.Main"]