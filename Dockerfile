FROM eclipse-temurin:21-jre

LABEL me.tejzs.project="ourmusic"

WORKDIR /app

COPY target/OurMusic-0.1.0.jar /app/ourmusic.jar

EXPOSE 8808

CMD ["java", "-Xms64m", "-Xmx256m", "-jar", "/app/ourmusic.jar"]
