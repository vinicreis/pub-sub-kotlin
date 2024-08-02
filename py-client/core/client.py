import grpc

from proto.io.github.vinicreis.pubsub.server.core.model.data.queue_pb2 import Queue
from proto.io.github.vinicreis.pubsub.server.core.model.request.list_request_pb2 import ListRequest
from proto.io.github.vinicreis.pubsub.server.core.service import queue_service_pb2_grpc


class Client:
    def __init__(self, address: str, port: int):
        self.channel = grpc.insecure_channel(f'{address}:{port}')
        self.stub = queue_service_pb2_grpc.QueueServiceStub(self.channel)

    def list(self) -> list[Queue]:
        return self.stub.list(ListRequest()).queues
