ENV_FILE_NAME=sample.env
PY_CLIENT_ROOT_PATH=py-client
PY_CLIENT_GENERATED_PATH=$(PY_CLIENT_ROOT_PATH)/proto
PROTOS_PATH=protos/src/main/proto/proto/io/github/vinicreis/pubsub/server/core
PROTOS_ROOT_PATH=proto/io/github/vinicreis/pubsub/server/core

.PHONY: server client py_client

build: server_build client_clean py_client_build
clean: server_clean client_clean py_client_clean

server: server_deploy

server_test:
	@echo "Running server tests..."
	@./gradlew test --rerun-tasks

server_build: server_clean
	@echo "Building server..."
	@./gradlew -q server:java-app:core:assembleDist

server_deploy: server_build
	@echo "Starting server..."
	@docker compose --env-file=$(ENV_FILE_NAME) up --quiet-pull -d --build

server_clean:
	@echo "Stopping server..."
	@docker compose --env-file=$(ENV_FILE_NAME) down
	@echo "Cleaning up server Gradle projects"
	@./gradlew -q server:core:domain:clean \
                  server:core:util:clean \
                  server:core:service:clean \
                  server:core:grpc:clean \
                  server:core:data:database:postgres:clean \
                  server:core:data:repository:clean \
                  server:core:test:clean \
                  server:java-app:core:clean

client: client_build
	@bin/pub-sub-client/bin/pub-sub-client

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
	@./gradlew -q client:core:domain:clean \
				  client:core:service:clean \
				  client:core:grpc:clean \
				  client:core:util:clean \
				  client:java-app:core:clean \
				  client:java-app:ui:cli:clean

py_client: py_client_build
	@echo "Running Python client..."
	@python $(PY_CLIENT_ROOT_PATH)/main.py

py_client_build: py_client_clean
	@echo "Generating gRPC Python files..."
	@mkdir -p $(PY_CLIENT_GENERATED_PATH)
	@python -m grpc_tools.protoc -I$(PROTOS_ROOT_PATH)=$(PROTOS_PATH) \
 		--python_out=$(PY_CLIENT_ROOT_PATH) \
 		 --pyi_out=$(PY_CLIENT_ROOT_PATH) \
 		  --grpc_python_out=$(PY_CLIENT_ROOT_PATH) \
 		  $(PROTOS_PATH)/**/*.proto \
 		  $(PROTOS_PATH)/**/**/*.proto

py_client_clean:
	@echo "Removing previous generated gRPC Python files..."
	@rm -rf $(PY_CLIENT_GENERATED_PATH)
