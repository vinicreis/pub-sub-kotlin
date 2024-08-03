import grpc

from core.grpc.mappers.queue_mapper import queue_to_remote
from core.grpc.mappers.response_mapper import publish_response_to_domain, list_response_to_domain
from core.grpc.response import Response
from core.model.queue import Queue
from core.model.text_message import TextMessage
from proto.io.github.vinicreis.pubsub.server.core.model.request.list_request_pb2 import ListRequest
from proto.io.github.vinicreis.pubsub.server.core.model.request.publish_request_pb2 import PublishRequest
from proto.io.github.vinicreis.pubsub.server.core.service.queue_service_pb2_grpc import QueueServiceStub


class Client:
    def __init__(self, address: str, port: int):
        self.channel = grpc.insecure_channel(f'{address}:{port}')
        self.stub = QueueServiceStub(self.channel)

    def list(self) -> Response:
        return list_response_to_domain(self.stub.list(ListRequest()))

    def publish(self, queue: Queue) -> Response:
        return publish_response_to_domain(self.stub.publish(PublishRequest(queue=queue_to_remote(queue))))

    def post(self, queue: Queue, text_message: TextMessage):
        return
