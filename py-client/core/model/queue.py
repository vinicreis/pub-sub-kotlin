from enum import Enum


class Queue:
    class Type(Enum):
        SIMPLE = 1
        MULTIPLE = 2

    def __init__(self, uuid: str, code: str, name: str, pending_messages_count: int):
        self.uuid = uuid
        self.code = code
        self.name = name
        self.pendingMessagesCount = pending_messages_count
