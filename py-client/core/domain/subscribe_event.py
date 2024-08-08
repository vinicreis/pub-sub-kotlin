from enum import Enum


class SubscriptionEvent(Enum):
    PROCESSING = 1
    ACTIVE = 2
    UPDATE = 3
    FINISHED = 4
