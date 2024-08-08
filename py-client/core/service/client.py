from core.domain.queue import Queue
from core.service.model.response import Response

class Client:
    def __init__(self, address: str, port: int):
        pass

    def list(self) -> Response:
        raise NotImplementedError

    def publish(self, queue: Queue) -> Response:
        raise NotImplementedError

    def post(self, queue: Queue, text_messages: list) -> Response:
        raise NotImplementedError

    def poll(self, queue: Queue, timeout_seconds: int | None = None) -> Response:
        raise NotImplementedError

    def subscribe(self, queue: Queue):
        raise NotImplementedError

    def remove(self, queue: Queue) -> Response:
        raise NotImplementedError
