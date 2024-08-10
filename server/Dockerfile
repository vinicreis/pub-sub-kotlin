FROM openjdk:17-jdk-alpine

ENV GRPC_HEALTH_PROBE_VERSION=v0.3.6
ARG SERVER_PORT
ADD /java-app/core/build/distributions/pub-sub-server-*.tar /app
WORKDIR /app
RUN wget -qO/bin/grpc_health_probe \
      https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/${GRPC_HEALTH_PROBE_VERSION}/grpc_health_probe-linux-amd64
RUN chmod +x /bin/grpc_health_probe
RUN mv pub-sub-server-* pub-sub-server
RUN chmod +x pub-sub-server/bin/pub-sub-server

ENTRYPOINT pub-sub-server/bin/pub-sub-server $SERVER_PORT
