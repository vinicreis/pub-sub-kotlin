DOCKER_COMPOSE_FILE := ./server/compose.yaml
PY_CLIENT_ROOT_PATH=py-client
PY_CLIENT_GENERATED_PATH=$(PY_CLIENT_ROOT_PATH)/proto
PROTOS_PATH=protos/src/main/proto/proto/io/github/vinicreis/pubsub/server/core
PROTOS_ROOT_PATH=proto/io/github/vinicreis/pubsub/server/core

.PHONY: server

server: deploy_server

build: server_build py_client_build

server_build:
	@echo "Building server..."
	@./gradlew -q assembleDist

deploy_server: server_build
	@echo "Starting server..."
	docker compose -f $(DOCKER_COMPOSE_FILE) up -d --build

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
