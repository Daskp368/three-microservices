# Учебный проект трех микросервисов

Система обработки заказов интернет-магазина на Java 17, Spring Boot, PostgreSQL, gRPC и Kafka. Проект запускается одной командой Docker Compose и содержит три приложения, три независимые PostgreSQL и один Kafka broker в режиме KRaft.

## Архитектура

```text
Client
  -> Order Service REST 8081 + JWT
       -> Order PostgreSQL 5433
       -> Inventory Service gRPC 9090
            -> Inventory PostgreSQL 5434
       -> Transactional Outbox
            -> Kafka orders 9092
                 -> Notification Service
                      -> Notification PostgreSQL 5435
                      -> read-only REST 8083
```

Каждый сервис владеет своей базой. Общих Entity, Repository и таблиц нет. Inventory Service проверяет остаток, но не резервирует и не уменьшает `quantity`. Order и Outbox сохраняются одной локальной транзакцией. Kafka-доставка имеет семантику at-least-once, поэтому Notification Service идемпотентен по `eventId`.

## Требования

- Docker Desktop с Docker Engine и Docker Compose;
- JDK 17 для локальной Maven-проверки;
- `curl` и `jq` для ручного сквозного сценария.

Глобальный Maven не требуется: проект содержит Maven Wrapper 3.3.4 и использует Maven 3.9.11.

## Быстрый запуск через Docker Compose

Из корня проекта создайте приватный файл окружения:

```bash
mkdir -p .env
cp .env.example .env/compose.properties
```

Заполните в `.env/compose.properties` как минимум:

- `ORDER_DB_PASSWORD`;
- `INVENTORY_DB_PASSWORD`;
- `NOTIFICATION_DB_PASSWORD`;
- `JWT_SECRET` длиной не менее 64 байт.

Пример генерации JWT-секрета:

```bash
openssl rand -hex 64
```

Приватный файл находится под `.env/`, игнорируется Git и не попадает в Docker build context.

Проверьте итоговую конфигурацию без запуска:

```bash
docker compose --env-file .env/compose.properties config --quiet
```

Соберите образы и запустите систему:

```bash
docker compose --env-file .env/compose.properties up --detach --build
docker compose --env-file .env/compose.properties ps
```

Ожидаются семь контейнеров со статусом `healthy`:

- `order-service`;
- `inventory-service`;
- `notification-service`;
- `order-postgres`;
- `inventory-postgres`;
- `notification-postgres`;
- `kafka`.

Логи всей системы:

```bash
docker compose --env-file .env/compose.properties logs --follow
```

Остановка с сохранением данных:

```bash
docker compose --env-file .env/compose.properties down
```

Команда ниже удаляет named volumes и все данные проекта. Выполняйте ее только для сознательной проверки чистого старта:

```bash
docker compose --env-file .env/compose.properties down --volumes --remove-orphans
```

## Docker-образы

У каждого приложения собственный многоэтапный Dockerfile. Build stage использует Maven 3.9.11 и JDK 17, собирает выбранный модуль вместе с reactor-зависимостями. Runtime stage содержит только Java 17 JRE и исполняемый JAR. Приложение запускается непривилегированным пользователем `spring`.

Сборка получает корневой каталог как context, потому что Order Service и Inventory Service зависят от модуля `contracts`. `.dockerignore` исключает Git-метаданные, настройки IDE, секреты `.env`, Maven `target` и документацию.

## Сеть, имена и порты

Внутри Compose контейнеры находят друг друга по service name. `localhost` внутри контейнера означает сам контейнер, поэтому используются адреса:

- `jdbc:postgresql://order-postgres:5432/order_db`;
- `jdbc:postgresql://inventory-postgres:5432/inventory_db`;
- `jdbc:postgresql://notification-postgres:5432/notification_db`;
- `inventory-service:9090` для gRPC;
- `kafka:19092` для приложений.

Хостовые порты привязаны только к `127.0.0.1`:

| Компонент | Порт хоста | Порт контейнера |
|---|---:|---:|
| Order REST | 8081 | 8081 |
| Inventory REST | 8082 | 8082 |
| Notification REST | 8083 | 8083 |
| Inventory gRPC | 9090 | 9090 |
| Kafka external listener | 9092 | 9092 |
| Order PostgreSQL | 5433 | 5432 |
| Inventory PostgreSQL | 5434 | 5432 |
| Notification PostgreSQL | 5435 | 5432 |

Kafka дополнительно слушает `19092` только внутри Docker-сети и `29093` для KRaft controller quorum.

## Healthcheck и порядок запуска

PostgreSQL проверяется через `pg_isready`, Kafka — через `kafka-topics.sh`, приложения — через доступные HTTP-маршруты. `depends_on` с `condition: service_healthy` не запускает зависимый сервис, пока инфраструктура действительно не готова принимать запросы.

Порядок зависимостей:

- Inventory Service ждёт Inventory PostgreSQL;
- Notification Service ждёт Notification PostgreSQL и Kafka;
- Order Service ждёт Order PostgreSQL, Kafka и здоровый Inventory Service.

Flyway выполняет миграции при старте приложения, после чего Hibernate проверяет схему через `ddl-auto=validate`.

## REST API

### Order Service

| Метод | Путь | Доступ |
|---|---|---|
| POST | `/auth/reg` | публичный |
| POST | `/auth/login` | публичный |
| POST | `/auth/refresh` | refresh token |
| GET, PUT, DELETE | `/api/users`, `/api/users/{userId}` | ADMIN |
| POST | `/api/order` | USER или ADMIN |

Создание пользователя выполняет `/auth/reg`; отдельный `POST /api/users` не добавляется.

### Inventory Service

| Метод | Путь |
|---|---|
| GET, POST | `/api/products` |
| GET, PUT, DELETE | `/api/products/{productId}` |

### Notification Service

| Метод | Путь |
|---|---|
| GET | `/api/orders/all` |
| GET | `/api/orders/by-order/{orderId}` |
| GET | `/api/orders/by-user/{userId}` |

Маршруты `by-order` и `by-user` устраняют конфликт двух одинаковых шаблонов из исходного ТЗ. Операций POST, PUT и DELETE у Notification API нет.

## Автоматические тесты

Перед контейнерной приёмкой выполните:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
./mvnw clean verify
```

Последняя полная проверка 2026-11-20:

| Модуль | Тесты | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Order Service | 70 | 0 | 0 | 0 |
| Inventory Service | 17 | 0 | 0 | 0 |
| Notification Service | 23 | 0 | 0 | 0 |
| **Всего** | **110** | **0** | **0** | **0** |

PostgreSQL integration-тесты используют Testcontainers. Kafka producer/consumer проверяются через Embedded Kafka. Для полного тестового запуска требуется работающий Docker Engine.

## Ручная приёмка

Полный воспроизводимый сценарий находится в `docs/e2e-scenario.md`. Фактический отчёт запуска — в `docs/final-acceptance.md`, а связь требований ТЗ с кодом и проверками — в `docs/requirements-traceability.md`.

## Работа с файлами в IntelliJ IDEA

Подробная учебная инструкция по `.git`, `.idea`, `.env`, `target`, `.gitignore` и `.dockerignore` находится в `docs/stages/stage-11-final-acceptance.md`.

Кратко:

- `.git` — служебный каталог Git; не удаляется и исключается только из Docker build context;
- `.idea` — локальные настройки IntelliJ IDEA; игнорируются Git и Docker;
- `.env` — приватные параметры и секреты; игнорируются Git и Docker, в репозитории хранится только `.env.example`;
- `target` — результат Maven-сборки; игнорируется Git и Docker и может быть пересоздан командой `./mvnw package`.

## Известные ограничения

- один Kafka broker и три одиночных PostgreSQL предназначены для учебной локальной среды, а не для production high availability;
- Kafka и базы используют plaintext внутри локальной Docker-сети;
- Inventory Service не резервирует остаток;
- вызовы нескольких товаров по gRPC выполняются последовательно;
- Kafka-доставка at-least-once, exactly-once не заявляется;
- DLT, Schema Registry, Avro, Saga, API Gateway, Kubernetes, метрики и distributed tracing не входят в исходное ТЗ;
- приватные значения передаются через environment variables, а не Docker Secrets.

## Документы проекта

- `docs/project-context.md` — актуальное состояние;
- `docs/progress.md` — прогресс этапов;
- `docs/decisions.md` — архитектурные решения;
- `docs/contracts/` — REST, gRPC, Kafka и модель данных;
- `docs/test-strategy.md` и `docs/test-scenarios.md` — стратегия и матрица тестов;
- `docs/final-acceptance.md` — финальная приёмка;
- `docs/requirements-traceability.md` — матрица соответствия ТЗ;
- `docs/review/teacher-feedback.md` — шаблон замечаний преподавателя.
