FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/nesting-assistant-starter/target/*.jar app.jar

ENV DB_HOST=postgres
ENV DB_PORT=5432
ENV DB_NAME=nesting_assistant
ENV DB_USERNAME=postgres
ENV DB_PASSWORD=postgres
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV WENXIN_API_KEY=your-api-key

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
