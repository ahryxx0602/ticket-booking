# ==============================================================================
# STAGE 1: Build JAR package with Maven & Eclipse Temurin JDK 21
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven Wrapper & POM files trước để tận dụng Docker layer caching
COPY pom.xml ./
COPY shared-kernel/pom.xml shared-kernel/
COPY context-identity/pom.xml context-identity/
COPY context-identity/identity-domain/pom.xml context-identity/identity-domain/
COPY context-identity/identity-application/pom.xml context-identity/identity-application/
COPY context-identity/identity-infrastructure/pom.xml context-identity/identity-infrastructure/
COPY context-identity/identity-presentation/pom.xml context-identity/identity-presentation/
COPY context-catalog/pom.xml context-catalog/
COPY context-catalog/catalog-domain/pom.xml context-catalog/catalog-domain/
COPY context-catalog/catalog-application/pom.xml context-catalog/catalog-application/
COPY context-catalog/catalog-infrastructure/pom.xml context-catalog/catalog-infrastructure/
COPY context-catalog/catalog-presentation/pom.xml context-catalog/catalog-presentation/
COPY context-booking/pom.xml context-booking/
COPY context-booking/booking-domain/pom.xml context-booking/booking-domain/
COPY context-booking/booking-application/pom.xml context-booking/booking-application/
COPY context-booking/booking-infrastructure/pom.xml context-booking/booking-infrastructure/
COPY context-booking/booking-presentation/pom.xml context-booking/booking-presentation/
COPY context-payment/pom.xml context-payment/
COPY context-payment/payment-domain/pom.xml context-payment/payment-domain/
COPY context-payment/payment-application/pom.xml context-payment/payment-application/
COPY context-payment/payment-infrastructure/pom.xml context-payment/payment-infrastructure/
COPY context-payment/payment-presentation/pom.xml context-payment/payment-presentation/
COPY context-notification/pom.xml context-notification/
COPY context-notification/notification-domain/pom.xml context-notification/notification-domain/
COPY context-notification/notification-application/pom.xml context-notification/notification-application/
COPY context-notification/notification-infrastructure/pom.xml context-notification/notification-infrastructure/
COPY context-notification/notification-presentation/pom.xml context-notification/notification-presentation/
COPY context-partner/pom.xml context-partner/
COPY context-partner/partner-domain/pom.xml context-partner/partner-domain/
COPY context-partner/partner-application/pom.xml context-partner/partner-application/
COPY context-partner/partner-infrastructure/pom.xml context-partner/partner-infrastructure/
COPY context-partner/partner-presentation/pom.xml context-partner/partner-presentation/
COPY ticket-start/pom.xml ticket-start/

# Copy toàn bộ mã nguồn
COPY shared-kernel/src shared-kernel/src
COPY context-identity/ context-identity/
COPY context-catalog/ context-catalog/
COPY context-booking/ context-booking/
COPY context-payment/ context-payment/
COPY context-notification/ context-notification/
COPY context-partner/ context-partner/
COPY ticket-start/src ticket-start/src

# Cài đặt maven và build trọn gói runnable jar
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -pl ticket-start -am

# ==============================================================================
# STAGE 2: Lightweight Runtime Container (Alpine JRE 21 < 180MB)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Tạo non-root user để tăng cường bảo mật container
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy jar thực thi từ builder stage
COPY --from=builder --chown=appuser:appgroup /build/ticket-start/target/ticket-start-*.jar app.jar

# Biến môi trường JVM tối ưu cho Virtual Threads và Container memory
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
