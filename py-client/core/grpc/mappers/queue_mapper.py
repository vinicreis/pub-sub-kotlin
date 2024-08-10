from core.domain.queue import Queue as DomainQueue
from proto.io.github.vinicreis.pubsub.server.core.model.data.queue_pb2 import Queue as RemoteQueue

def queue_type_to_domain(remote_queue_type: RemoteQueue.Type) -> DomainQueue.Type:
    if remote_queue_type == RemoteQueue.Type.SIMPLE:
        return DomainQueue.Type.SIMPLE
    elif remote_queue_type == RemoteQueue.Type.MULTIPLE:
        return DomainQueue.Type.MULTIPLE
    else:
        raise ValueError(f"Unknown queue type: {remote_queue_type}")

def queue_type_to_remote(domain_queue_type: DomainQueue.Type) -> RemoteQueue.Type:
    if domain_queue_type == DomainQueue.Type.SIMPLE:
        return RemoteQueue.Type.SIMPLE
    elif domain_queue_type == DomainQueue.Type.MULTIPLE:
        return RemoteQueue.Type.MULTIPLE
    else:
        raise ValueError(f"Unknown queue type: {domain_queue_type}")

def queue_to_domain(remote_queue: RemoteQueue) -> DomainQueue:
    return DomainQueue(
        guid=remote_queue.id,
        code=remote_queue.code,
        name=remote_queue.name,
        queue_type=queue_type_to_domain(remote_queue.type),
        pending_messages_count=remote_queue.pendingMessagesCount,
    )


def queue_to_remote(queue: DomainQueue) -> RemoteQueue:
    return RemoteQueue(
        id=queue.guid,
        code=queue.code,
        name=queue.name,
        type=queue_type_to_remote(queue.queue_type),
        pendingMessagesCount=queue.pending_messages_count,
    )
