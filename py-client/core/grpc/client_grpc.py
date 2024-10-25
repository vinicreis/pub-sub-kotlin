import grpc

from core.domain.queue import Queue
from core.grpc.mappers.queue_mapper import queue_to_remote
from core.grpc.mappers.response_mapper import publish_response_to_domain, list_response_to_domain, \
    post_response_to_domain, poll_response_to_domain, remove_response_to_domain, subscribe_response_to_domain
from core.grpc.mappers.text_message_mapper import text_message_to_remote
from core.service.client import Client
from core.service.model.response import Response
from proto.io.github.vinicreis.pubsub.server.core.model.request.list_request_pb2 import ListRequest
from proto.io.github.vinicreis.pubsub.server.core.model.request.poll_request_pb2 import PollRequest
from proto.io.github.vinicreis.pubsub.server.core.model.request.post_request_pb2 import PostRequest
from proto.io.github.vinicreis.pubsub.server.core.model.request.publish_request_pb2 import PublishRequest
from proto.io.github.vinicreis.pubsub.server.core.model.request.remove_request_pb2 import RemoveRequest
from proto.io.github.vinicreis.pubsub.server.core.model.request.subscribe_request_pb2 import SubscribeRequest
from proto.io.github.vinicreis.pubsub.server.core.service.queue_service_pb2_grpc import QueueServiceStub


class ClientGrpc(Client):
    def __init__(self, address: str, port: int):
        super().__init__(address, port)
        self.channel = grpc.insecure_channel(f'{address}:{port}')
        self.stub = QueueServiceStub(self.channel)

    def list(self) -> Response:
        return list_response_to_domain(self.stub.list(ListRequest()))

    def publish(self, queue: Queue) -> Response:
        return publish_response_to_domain(self.stub.publish(PublishRequest(queue=queue_to_remote(queue))))

    def post(self, queue: Queue, text_messages: list) -> Response:
        remote_text_messages = list(map(text_message_to_remote, text_messages))

        if len(text_messages) < 1:
            raise ValueError("No messages to post")
        else:
            return post_response_to_domain(
                self.stub.post(
                    PostRequest(queueId=queue.guid, content=remote_text_messages)
                )
            )

    def poll(self, queue: Queue, timeout_seconds: int | None = None) -> Response:
        return poll_response_to_domain(self.stub.poll(PollRequest(queueId=queue.guid), timeout=timeout_seconds))

    def subscribe(self, queue: Queue, timeout_seconds: int | None = None) -> iter:
        return iter(map(subscribe_response_to_domain, self.stub.subscribe(SubscribeRequest(queueId=queue.guid), timeout=timeout_seconds)))

    def remove(self, queue: Queue) -> Response:
        return remove_response_to_domain(self.stub.remove(RemoveRequest(id=queue.guid)))
