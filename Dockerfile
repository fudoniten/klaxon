FROM clojure:temurin-21-tools-deps AS builder

WORKDIR /app
COPY deps.edn .
RUN clojure -P
COPY . .
RUN clojure -T:uber uber

FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app
COPY --from=builder /app/target/

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
