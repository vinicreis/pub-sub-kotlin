import uuid
from enum import Enum


class Queue:
    class Type(Enum):
        SIMPLE = 1
        MULTIPLE = 2

    def __init__(self,
                 code: str,
                 name: str,
                 queue_type: Type,
                 guid: str = str(uuid.uuid4()),
                 pending_messages_count: int = 0):
        self.guid = guid
        self.code = code
        self.name = name
        self.queue_type = queue_type
        self.pending_messages_count = pending_messages_count

    def __str__(self):
        return (f"\tQueue {self.name}\n" +
                f"\t\tID: {self.guid}\n" +
                f"\t\tCode: {self.name}\n" +
                f"\t\tType: {self.queue_type}\n" +
                f"\t\tHas {self.pending_messages_count} pending messages\n")
