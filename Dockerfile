# ==============================================================================
# STAGE 1: Build JAR package with Maven & Eclipse Temurin JDK 21
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven Wrapper & POM files
COPY backend/pom.xml ./
COPY backend/shared-kernel/pom.xml shared-kernel/
COPY backend/context-identity/pom.xml context-identity/
COPY backend/context-identity/identity-domain/pom.xml context-identity/identity-domain/
COPY backend/context-identity/identity-application/pom.xml context-identity/identity-application/
COPY backend/context-identity/identity-infrastructure/pom.xml context-identity/identity-infrastructure/
COPY backend/context-identity/identity-presentation/pom.xml context-identity/identity-presentation/
COPY backend/context-catalog/pom.xml context-catalog/
COPY backend/context-catalog/catalog-domain/pom.xml context-catalog/catalog-domain/
COPY backend/context-catalog/catalog-application/pom.xml context-catalog/catalog-application/
COPY backend/context-catalog/catalog-infrastructure/pom.xml context-catalog/catalog-infrastructure/
COPY backend/context-catalog/catalog-presentation/pom.xml context-catalog/catalog-presentation/
COPY backend/context-booking/pom.xml context-booking/
COPY backend/context-booking/booking-domain/pom.xml context-booking/booking-domain/
COPY backend/context-booking/booking-application/pom.xml context-booking/booking-application/
COPY backend/context-booking/booking-infrastructure/pom.xml context-booking/booking-infrastructure/
COPY backend/context-booking/booking-presentation/pom.xml context-booking/booking-presentation/
COPY backend/context-payment/pom.xml context-payment/
COPY backend/context-payment/payment-domain/pom.xml context-payment/payment-domain/
COPY backend/context-payment/payment-application/pom.xml context-payment/payment-application/
COPY backend/context-payment/payment-infrastructure/pom.xml context-payment/payment-infrastructure/
COPY backend/context-payment/payment-presentation/pom.xml context-payment/payment-presentation/
COPY backend/context-notification/pom.xml context-notification/
COPY backend/context-notification/notification-domain/pom.xml context-notification/notification-domain/
COPY backend/context-notification/notification-application/pom.xml context-notification/notification-application/
COPY backend/context-notification/notification-infrastructure/pom.xml context-notification/notification-infrastructure/
COPY backend/context-notification/notification-presentation/pom.xml context-notification/notification-presentation/
COPY backend/context-partner/pom.xml context-partner/
COPY backend/context-partner/partner-domain/pom.xml context-partner/partner-domain/
COPY backend/context-partner/partner-application/pom.xml context-partner/partner-application/
COPY backend/context-partner/partner-infrastructure/pom.xml context-partner/partner-infrastructure/
COPY backend/context-partner/partner-presentation/pom.xml context-partner/partner-presentation/
COPY backend/ticket-start/pom.xml ticket-start/

# Copy toàn bộ mã nguồn
COPY backend/shared-kernel/src shared-kernel/src
COPY backend/context-identity/ context-identity/
COPY backend/context-catalog/ context-catalog/
COPY backend/context-booking/ context-booking/
COPY backend/context-payment/ context-payment/
COPY backend/context-notification/ context-notification/
COPY backend/context-partner/ context-partner/
COPY backend/ticket-start/src ticket-start/src

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
