Autorizacion de Pagos - Cooperativa

Puertos (host): API 8081, PostgreSQL 5442, RabbitMQ 5672 y 15672.
Swagger: http://localhost:8081/swagger-ui.html
Healthcheck: http://localhost:8081/actuator/health

Como levantar:
docker compose down -v
docker compose up -d --build

Ver estado:
docker compose ps

Pago normal:
curl -X POST http://localhost:8081/api/v1/pagos -H "Content-Type: application/json" -H "X-Idempotency-Key: key-1" -H "X-Usuario: usuario-test" -d '{"numeroSocio":"S-1001","monto":100.00,"referencia":"REF-1"}'

Repetir misma clave:
curl -X POST http://localhost:8081/api/v1/pagos -H "Content-Type: application/json" -H "X-Idempotency-Key: key-1" -H "X-Usuario: usuario-test" -d '{"numeroSocio":"S-1001","monto":100.00,"referencia":"REF-1"}'

Saldo insuficiente:
curl -X POST http://localhost:8081/api/v1/pagos -H "Content-Type: application/json" -H "X-Idempotency-Key: key-insuf" -H "X-Usuario: usuario-test" -d '{"numeroSocio":"S-1003","monto":500.00,"referencia":"REF-2"}'

Limite excedido:
curl -X POST http://localhost:8081/api/v1/pagos -H "Content-Type: application/json" -H "X-Idempotency-Key: key-lim" -H "X-Usuario: usuario-test" -d '{"numeroSocio":"S-1001","monto":1500.00,"referencia":"REF-3"}'

Banco con BANCO_SIMULAR_DEMORA_MS=5000 (debe dar 202 PENDIENTE_BANCO):
BANCO_SIMULAR_DEMORA_MS=5000 docker compose up -d --build
curl -X POST http://localhost:8081/api/v1/pagos -H "Content-Type: application/json" -H "X-Idempotency-Key: key-pend-1" -H "X-Usuario: usuario-test" -d '{"numeroSocio":"S-1001","monto":50.00,"referencia":"REF-PEND"}'
