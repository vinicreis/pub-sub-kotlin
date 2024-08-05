import grpc

from core.grpc.mappers.queue_mapper import queue_to_remote
from core.grpc.mappers.response_mapper import publish_response_to_domain, list_response_to_domain, \
    post_response_to_domain, poll_response_to_domain, remove_response_to_domain, subscribe_response_to_domain
from core.grpc.response import Response
from core.model.queue import Queue
from core.model.text_message import TextMessage
from proto.io.github.vinicreis.pubsub.server.core.model.request.list_request_pb2 import ListRequest
from proto.io.github.vinicreis.pubsub.server.core.model.request.poll_request_pb2 import PollRequest
from proto.io.github.vinicreis.pubsub.server.core.model.request.post_multiple_request_pb2 import PostMultipleRequest
from proto.io.github.vinicreis.pubsub.server.core.model.request.post_single_request_pb2 import PostSingleRequest
from proto.io.github.vinicreis.pubsub.server.core.model.request.publish_request_pb2 import PublishRequest
from proto.io.github.vinicreis.pubsub.server.core.model.request.remove_request_pb2 import RemoveRequest
from proto.io.github.vinicreis.pubsub.server.core.model.request.subscribe_request_pb2 import SubscribeRequest
from proto.io.github.vinicreis.pubsub.server.core.service.queue_service_pb2_grpc import QueueServiceStub


class Client:
    def __init__(self, address: str, port: int):
        self.channel = grpc.insecure_channel(f'{address}:{port}')
        self.stub = QueueServiceStub(self.channel)

    def list(self) -> Response:
        return list_response_to_domain(self.stub.list(ListRequest()))

    def publish(self, queue: Queue) -> Response:
        return publish_response_to_domain(self.stub.publish(PublishRequest(queue=queue_to_remote(queue))))

    def post(self, queue: Queue, text_messages: list[TextMessage]) -> Response:
        if len(text_messages) > 1:
            return post_response_to_domain(
                self.stub.postMultiple(
                    PostMultipleRequest(queueId=queue.guid, content=text_messages)
                )
            )
        elif len(text_messages) == 1:
            return post_response_to_domain(
                self.stub.postSingle(
                    PostSingleRequest(queueId=queue.guid, content=text_messages[0])
                )
            )
        else:
            raise ValueError("No messages to post")

    def poll(self, queue: Queue, timeout_seconds: int | None = None) -> Response:
        return poll_response_to_domain(self.stub.poll(PollRequest(queueId=queue.guid, timeoutSeconds=timeout_seconds)))

    def subscribe(self, queue: Queue):
        return map(subscribe_response_to_domain, self.stub.subscribe(SubscribeRequest(queueId=queue.guid)))

    def remove(self, queue: Queue) -> Response:
        return remove_response_to_domain(self.stub.remove(RemoveRequest(id=queue.guid)))
