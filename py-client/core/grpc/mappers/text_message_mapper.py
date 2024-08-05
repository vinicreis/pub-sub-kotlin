from core.model.text_message import TextMessage as DomainTextMessage
from proto.io.github.vinicreis.pubsub.server.core.model.data.text_message_pb2 import TextMessage as RemoteTextMessage


def text_message_to_domain(text_message: RemoteTextMessage) -> DomainTextMessage:
    return DomainTextMessage(
        content=text_message.content,
    )


def text_message_to_remote(text_message: DomainTextMessage) -> RemoteTextMessage:
    return RemoteTextMessage(
        content=text_message.content,
    )
