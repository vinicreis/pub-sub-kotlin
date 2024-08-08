class TextMessage:
    def __init__(self, content: str):
        self.content = content

    def __str__(self):
        return self.content
