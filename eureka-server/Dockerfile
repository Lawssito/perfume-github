FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copiar todos los pom.xml (necesarios para el reactor de Maven)
COPY pom.xml ./
COPY api-gateway/pom.xml api-gateway/
COPY eureka-server/pom.xml eureka-server/
COPY auth-service/pom.xml auth-service/
COPY security-service/pom.xml security-service/
COPY user-service/pom.xml user-service/
COPY ms-carrito/pom.xml ms-carrito/
COPY ms-catalogo/pom.xml ms-catalogo/
COPY ms-pedidos/pom.xml ms-pedidos/
COPY ms-pagos/pom.xml ms-pagos/
COPY ms-envios/pom.xml ms-envios/
COPY ms-stock/pom.xml ms-stock/
COPY ms-notificaciones/pom.xml ms-notificaciones/

# Copiar solo el source de este modulo
COPY eureka-server/src eureka-server/src

RUN mvn clean package -DskipTests -pl eureka-server

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/eureka-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]