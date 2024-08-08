from core.domain.subscribe_event import SubscriptionEvent
from core.grpc.mappers.queue_mapper import queue_to_domain
from core.grpc.mappers.text_message_mapper import text_message_to_domain
from core.service.model.response import Response, SubscribeResponse
from proto.io.github.vinicreis.pubsub.server.core.model.data.result_pb2 import Result
from proto.io.github.vinicreis.pubsub.server.core.model.data.subscription_event_pb2 import SubscriptionEvent \
    as RemoteSubscriptionEvent
from proto.io.github.vinicreis.pubsub.server.core.model.response.list_response_pb2 import ListResponse
from proto.io.github.vinicreis.pubsub.server.core.model.response.poll_response_pb2 import PollResponse
from proto.io.github.vinicreis.pubsub.server.core.model.response.post_response_pb2 import PostResponse
from proto.io.github.vinicreis.pubsub.server.core.model.response.publish_response_pb2 import PublishResponse
from proto.io.github.vinicreis.pubsub.server.core.model.response.remove_response_pb2 import RemoveResponse
from proto.io.github.vinicreis.pubsub.server.core.model.response.subscribe_response_pb2 import \
    SubscribeResponse as RemoteSubscribeResponse


def list_response_to_domain(remote_response: ListResponse) -> Response:
    if remote_response.result == Result.SUCCESS:
        return Response(
            result=Response.Result.SUCCESS,
            data=list(map(queue_to_domain, remote_response.queues)),
        )
    elif remote_response.result == Result.ERROR:
        return Response(
            result=Response.Result.FAIL,
            data=None,
            error=remote_response.message,
        )
    else:
        raise ValueError(f"Unknown response: {remote_response}")


def publish_response_to_domain(remote_response: PublishResponse) -> Response:
    if remote_response.result == Result.SUCCESS:
        return Response(
            result=Response.Result.SUCCESS,
            data=queue_to_domain(remote_response.queue),
        )
    elif remote_response.result == Result.ERROR:
        return Response(
            result=Response.Result.FAIL,
            data=None,
            error=remote_response.message,
        )
    else:
        raise ValueError(f"Unknown response: {remote_response}")


def post_response_to_domain(remote_response: PostResponse) -> Response:
    if remote_response.result == Result.SUCCESS:
        return Response(
            result=Response.Result.SUCCESS,
            data=queue_to_domain(remote_response.queue),
        )
    elif remote_response.result == Result.ERROR:
        return Response(
            result=Response.Result.FAIL,
            data=None,
            error=remote_response.message,
        )
    else:
        raise ValueError(f"Unknown response: {remote_response}")


def remove_response_to_domain(remote_response: RemoveResponse) -> Response:
    if remote_response.result == Result.SUCCESS:
        return Response(
            result=Response.Result.SUCCESS,
            data=queue_to_domain(remote_response.queue),
        )
    elif remote_response.result == Result.ERROR:
        return Response(
            result=Response.Result.FAIL,
            data=None,
            error=remote_response.message,
        )
    else:
        raise ValueError(f"Unknown response: {remote_response}")


def poll_response_to_domain(remote_response: PollResponse) -> Response:
    if remote_response.result == Result.SUCCESS:
        return Response(
            result=Response.Result.SUCCESS,
            data=text_message_to_domain(remote_response.content),
        )
    elif remote_response.result == Result.ERROR:
        return Response(
            result=Response.Result.FAIL,
            data=None,
            error=remote_response.message,
        )
    else:
        raise ValueError(f"Unknown response: {remote_response}")


def event_to_domain(remove_event: RemoteSubscriptionEvent) -> SubscriptionEvent:
    if remove_event == RemoteSubscriptionEvent.PROCESSING:
        return SubscriptionEvent.PROCESSING
    elif remove_event == RemoteSubscriptionEvent.ACTIVE:
        return SubscriptionEvent.ACTIVE
    elif remove_event == RemoteSubscriptionEvent.UPDATE:
        return SubscriptionEvent.UPDATE
    elif remove_event == RemoteSubscriptionEvent.FINISHED:
        return SubscriptionEvent.FINISHED
    else:
        raise ValueError(f"Unknown subscription event: {remove_event}")


def subscribe_response_to_domain(remote_response: RemoteSubscribeResponse) -> SubscribeResponse:
    return SubscribeResponse(
        event=event_to_domain(remote_response.event),
        queue=queue_to_domain(remote_response.queue),
        text_message=text_message_to_domain(remote_response.content),
        message=remote_response.message,
    )
