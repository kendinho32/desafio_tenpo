# Desafío Tenpo — API reactiva de cálculo con porcentaje dinámico

API REST construida con Spring Boot WebFlux (Java 21) que suma dos números y les aplica un porcentaje dinámico obtenido de un servicio simulado, con caché en Redis, reintentos, historial de llamadas paginado y asíncrono, y rate limiting global.

## Cómo desplegar localmente

Requisitos: Docker y Docker Compose.

```bash
docker compose up --build
```

Esto levanta tres contenedores: `api` (puerto 8080), `postgres` (puerto 5432) y `redis` (puerto 6379). El esquema de base de datos se crea automáticamente al arrancar.

## Cómo probar los endpoints

### Swagger UI

http://localhost:8080/swagger-ui.html

### Colección Postman

Importa `.docs/postman/tenpo-challenge.postman_collection.json` en Postman.

### Curl

```bash
curl -X POST http://localhost:8080/api/v1/calculations \
  -H "Content-Type: application/json" \
  -d '{"num1": 5, "num2": 5}'

curl "http://localhost:8080/api/v1/history?page=0&size=20"
```

## Publicar la imagen en Docker Hub

```bash
docker build -t <TU_USUARIO_DOCKERHUB>/tenpo-challenge:latest .
docker login
docker push <TU_USUARIO_DOCKERHUB>/tenpo-challenge:latest
```

## Ejecutar los tests

```bash
./gradlew test
```

Requiere un daemon de Docker local activo (los tests de integración usan Testcontainers para levantar Postgres y Redis efímeros).

## Análisis técnico (bonus)

- **WebFlux + R2DBC + Redis reactivo de punta a punta**: se eligió una pila 100% no bloqueante porque el enunciado lo exige explícitamente y porque es la única forma de aprovechar WebFlux bajo carga real — mezclar JDBC bloqueante habría anulado la ventaja de usar un runtime reactivo.
- **Redis para caché y rate limiting**: ambos requisitos (caché del porcentaje, límite de 3 RPM) deben seguir siendo correctos si la API corre con múltiples réplicas. Un caché o contador en memoria local se rompe en ese escenario porque cada réplica tendría su propia copia. Redis centraliza ambos estados.
- **Rate limit con ventana fija (no deslizante)**: se implementó con `INCR` + `EXPIRE` sobre una clave por minuto epoch. Es más simple y barato que una ventana deslizante, a costa de permitir ráfagas cerca del borde de la ventana (p. ej. 3 requests al segundo 59 y 3 más al segundo 61). Para el alcance de este desafío es una compensación aceptable; una ventana deslizante (sorted sets de Redis) sería el siguiente paso si el requisito de precisión fuera más estricto.
- **Rate limit acotado a `/api/**`**: el filtro solo cuenta solicitudes cuyo path empieza con `/api/`; el resto (Swagger UI, `/v3/api-docs` y sus recursos estáticos) queda exento. Sin este recorte, abrir `http://localhost:8080/swagger-ui.html` dispara varias solicitudes internas (redirect, JS/CSS del bundle, `swagger-config`, `api-docs`) que por sí solas superan el límite de 3 y dejan la página a medio cargar con 429s.
- **Rate limit "fail-open" ante una caída de Redis**: si `INCR`/`EXPIRE` fallan (Redis inalcanzable), el filtro deja pasar la solicitud en lugar de responder con un error genérico de Spring (que rompería el contrato uniforme de `ApiError`). Se prioriza la disponibilidad de la API de negocio sobre la precisión del límite de tasa durante una incidencia de infraestructura; el evento queda registrado con `log.warn`.
- **Historial vía bus de eventos interno (`Sinks.Many`) en lugar de persistencia inline**: el enunciado exige que el registro de llamadas sea asíncrono y que un fallo de escritura no afecte al endpoint de negocio. Publicar el evento y seguir sin esperar la escritura (con un único suscriptor de fondo que atrapa sus propios errores) cumple ambos requisitos sin acoplar la solicitud HTTP a la disponibilidad de Postgres.
- **Suscripción del historial resiliente a errores no previstos**: además de que `persist()` atrapa sus propios fallos, la suscripción de fondo agrega `doOnError` + `retry()` como red de seguridad ante cualquier error que escape igualmente (p. ej. un bug al mapear el evento). El sink se crea con `onBackpressureBuffer(256, false)` (`autoCancel=false`) a propósito: con el valor por defecto (`autoCancel=true`), el sink se termina para siempre en cuanto su único suscriptor desaparece — incluso en el breve instante en que `retry()` se está resuscribiendo — dejando `publish()` fallando en silencio de ahí en adelante. `autoCancel=false` es lo que hace que `retry()` realmente sirva.
- **Caché sin expiración dura**: el porcentaje se considera "fresco" durante 30 minutos (se usa sin llamar al servicio externo), pero el valor en sí nunca se borra automáticamente — así puede servir de último recurso ante fallos del servicio externo incluso después de que expiró su frescura, tal como pide el enunciado ("usar el último valor almacenado").
- **Reintentos con `Retry.backoff` de Reactor**: se usó el mecanismo nativo de Reactor en lugar de una librería externa (Resilience4j) porque cubre exactamente el caso de uso (reintento con backoff sobre un `Mono`) sin añadir una dependencia adicional.
- **Sin autenticación**: el enunciado no la solicita; agregarla habría sido alcance no pedido.
