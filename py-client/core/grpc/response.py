from enum import Enum

from core.grpc.subscribe_event import SubscribeEvent
from core.model.queue import Queue
from core.model.text_message import TextMessage


class Response[T]:
    class Result(Enum):
        FAIL = -1
        SUCCESS = 0

    def __init__(self, result: Result, data: T, error: str = None):
        self.result = result
        self.data = data
        self.error = error


class SubscribeResponse:
    def __init__(self, event: SubscribeEvent, queue: Queue, text_message: TextMessage, message: str):
        self.event = event
        self.queue = queue
        self.textMessage = text_message
        self.message = message
