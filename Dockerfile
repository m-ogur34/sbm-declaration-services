# The jar is produced by "mvnw clean verify" on the CI agent, which is the only place with
# access to the internal Nexus, so this image only carries the runtime.
FROM registry.allianz.com.tr/base/eclipse-temurin:21-jre-alpine

ARG JAR_FILE=target/sbm-declaration-services.jar

ENV TZ=Europe/Istanbul \
    LANG=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.timezone=Europe/Istanbul"

WORKDIR /app

COPY ${JAR_FILE} /app/app.jar

RUN addgroup -S allianz && adduser -S allianz -G allianz && chown -R allianz:allianz /app
USER allianz

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O - http://localhost:8080/sbm-declaration-services/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_ARGS -jar /app/app.jar"]
