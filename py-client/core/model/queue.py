import uuid
from enum import Enum


class Queue:
    class Type(Enum):
        SIMPLE = 1
        MULTIPLE = 2

    def __init__(self, code: str, name: str, guid: str = uuid.uuid4(), pending_messages_count: int = 0):
        self.guid = guid
        self.code = code
        self.name = name
        self.pending_messages_count = pending_messages_count

    def __str__(self):
        return f"""
            Queue {self.code} 
                id={self.guid}
                name={self.name}
                Has {self.pending_messages_count} pending messages
        """
