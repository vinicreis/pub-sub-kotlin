from enum import Enum

from core.domain.queue import Queue
from core.domain.subscribe_event import SubscriptionEvent
from core.domain.text_message import TextMessage


class Response[T]:
    class Result(Enum):
        FAIL = -1
        SUCCESS = 0

    def __init__(self, result: Result, data: T, error: str = None):
        self.result = result
        self.data = data
        self.error = error


class SubscribeResponse:
    def __init__(self, event: SubscriptionEvent, queue: Queue, text_message: TextMessage, message: str):
        self.event = event
        self.queue = queue
        self.textMessage = text_message
        self.message = message

    def __str__(self):
        if self.event == SubscriptionEvent.PROCESSING:
            return f"Processing subscription on queue {self.queue.code}"
        elif self.event == SubscriptionEvent.ACTIVE:
            return f"Subscription on queue {self.queue.code} is active!"
        elif self.event == SubscriptionEvent.UPDATE:
            return f"Received message on queue {self.queue.code}: \"{self.textMessage}\""
        elif self.event == SubscriptionEvent.FINISHED:
            return f"Subscription on queue {self.queue.code} finished: ${self.message}"
        else:
            raise ValueError(f"Unknown subscription event: {self.event}")
