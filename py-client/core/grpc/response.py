from enum import Enum


class Response[T]:
    class Result(Enum):
        FAIL = -1
        SUCCESS = 0

    def __init__(self, result: Result, data: T, error: str = None):
        self.result = result
        self.data = data
        self.error = error
