FROM maven:3.9.4-amazoncorretto-21 as build-api
WORKDIR /tmp/api
COPY . .
RUN mvn clean install -DskipTests

FROM amazoncorretto:21-alpine as build-jre
WORKDIR /tmp/jre
# required for strip-debug to work
RUN apk add --no-cache binutils
RUN jlink \
         --add-modules ALL-MODULE-PATH \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output minimal

FROM alpine:latest
WORKDIR /app
ENV JAVA_HOME=/jre
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XshowSettings:vm -XX:+PrintCommandLineFlags -XX:+TieredCompilation"
ENV PATH="$PATH:$JAVA_HOME/bin"
RUN apk update && apk add ca-certificates openssl
COPY --from=build-jre /tmp/jre/minimal $JAVA_HOME
COPY --from=build-api /tmp/api/target/clin-portal-forms-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT java $JAVA_OPTS -jar app.jar

# legacy way to run the app without 'build-jre' custom JRE
#FROM openjdk:17-alpine
#WORKDIR /app
#RUN apk update && apk add ca-certificates openssl
#COPY --from=build-api /tmp/api/target/clin-portal-forms-0.0.1-SNAPSHOT.jar app.jar
#EXPOSE 8080
#ENTRYPOINT ["java","-jar","app.jar"]
