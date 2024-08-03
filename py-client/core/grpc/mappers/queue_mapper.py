from core.model.queue import Queue as DomainQueue
from proto.io.github.vinicreis.pubsub.server.core.model.data.queue_pb2 import Queue as RemoteQueue


def queue_to_domain(remote_queue: RemoteQueue) -> DomainQueue:
    return DomainQueue(
        uuid=remote_queue.id,
        code=remote_queue.code,
        name=remote_queue.name,
        pending_messages_count=remote_queue.pendingMessagesCount,
    )


def queue_to_remote(queue: DomainQueue) -> RemoteQueue:
    return RemoteQueue(
        id=queue.uuid,
        code=queue.code,
        name=queue.name,
        pendingMessagesCount=queue.pendingMessagesCount,
    )
