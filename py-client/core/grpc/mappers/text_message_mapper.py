from core.model.text_message import TextMessage as DomainTextMessage
from proto.io.github.vinicreis.pubsub.server.core.model.data.text_message_pb2 import TextMessage as RemoteTextMessage


def text_message_to_domain(text_message: RemoteTextMessage) -> DomainTextMessage:
    return DomainTextMessage(
        uuid=text_message.id,
        content=text_message.content,
    )
