GRADLE_TASK := assembleDist
DOCKER_COMPOSE_FILE := ./server/compose.yaml

.PHONY: all build server

all: build server

build:
	./gradlew $(GRADLE_TASK)

server:
	docker-compose -f $(DOCKER_COMPOSE_FILE) up --build
