FROM openjdk:17-jdk-alpine

ARG PORT
ADD /java-app/core/build/distributions/pub-sub-server-*.tar /app
WORKDIR /app
RUN mv pub-sub-server-* pub-sub-server
RUN chmod +x pub-sub-server/bin/pub-sub-server

ENTRYPOINT ["pub-sub-server/bin/pub-sub-server", "$PORT"]
