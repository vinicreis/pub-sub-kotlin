DOCKER_COMPOSE_FILE := ./server/compose.yaml
PY_CLIENT_ROOT_PATH=py-client
PY_CLIENT_GENERATED_PATH=$(PY_CLIENT_ROOT_PATH)/proto
PROTOS_PATH=protos/src/main/proto/proto/io/github/vinicreis/pubsub/server/core
PROTOS_ROOT_PATH=proto/io/github/vinicreis/pubsub/server/core

.PHONY: server

server: server_deploy

build: server_build client_clean py_client_build

clean: server_clean client_clean

server_build:
	@echo "Building server..."
	@./gradlew -q server:java-app:core:assembleDist

server_deploy: server_build
	@echo "Starting server..."
	docker compose -f $(DOCKER_COMPOSE_FILE) up -d --build

server_clean:
	@echo "Stopping server..."
	@docker compose -f $(DOCKER_COMPOSE_FILE) down
	@echo "Cleaning up server Gradle projects"
	@./gradlew -q server:java-app:core:clean

client_build: client_clean
	@echo "Building client..."
	@./gradlew -q client:java-app:core:assembleDist
	@echo "Moving ang unpacking client executable..."
	@mkdir -p bin
	@cp client/java-app/core/build/distributions/*.tar bin/client.tar
	@tar -xf bin/client.tar -C bin
	@rm bin/*.tar
	@mv bin/pub-sub-client* bin/pub-sub-client

client_clean:
	@rm -rf bin
	@echo "Cleaning up client Gradle projects"
	@./gradlew -q client:java-app:core:clean

py_client_build:
	@echo "Removing previous generated gRPC Python files..."
	@rm -rf $(PY_CLIENT_GENERATED_PATH)
	@echo "Generating gRPC Python files..."
	@mkdir -p $(PY_CLIENT_GENERATED_PATH)
	@touch $(PY_CLIENT_GENERATED_PATH)/__init__.py
	@python -m grpc_tools.protoc -I$(PROTOS_ROOT_PATH)=$(PROTOS_PATH) \
 		--python_out=$(PY_CLIENT_ROOT_PATH) \
 		 --pyi_out=$(PY_CLIENT_ROOT_PATH) \
 		  --grpc_python_out=$(PY_CLIENT_ROOT_PATH) \
 		  $(PROTOS_PATH)/**/*.proto \
 		  $(PROTOS_PATH)/**/**/*.proto
