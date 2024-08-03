from core.grpc.mappers.queue_mapper import queue_to_domain
from core.grpc.response import Response, SubscribeResponse
from core.grpc.subscribe_event import SubscribeEvent
from proto.io.github.vinicreis.pubsub.server.core.model.data.result_pb2 import Result
from proto.io.github.vinicreis.pubsub.server.core.model.response.list_response_pb2 import ListResponse
from proto.io.github.vinicreis.pubsub.server.core.model.response.post_response_pb2 import PostResponse
from proto.io.github.vinicreis.pubsub.server.core.model.response.publish_response_pb2 import PublishResponse
from proto.io.github.vinicreis.pubsub.server.core.model.response.remove_response_pb2 import RemoveResponse
from proto.io.github.vinicreis.pubsub.server.core.model.response.subscribe_response_pb2 import \
    SubscribeResponse as RemoteSubscribeResponse


def list_response_to_domain(remote_response: ListResponse) -> Response:
    if remote_response.result == Result.SUCCESS:
        return Response(
            result=Response.Result.SUCCESS,
            data=map(queue_to_domain, remote_response.queues),
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
            data=remote_response.queue,
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
            data=remote_response.queue,
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
            data=remote_response.queue,
        )
    elif remote_response.result == Result.ERROR:
        return Response(
            result=Response.Result.FAIL,
            data=None,
            error=remote_response.message,
        )
    else:
        raise ValueError(f"Unknown response: {remote_response}")


def subscribe_response_to_domain(remote_response: RemoteSubscribeResponse) -> SubscribeResponse:
    return SubscribeResponse(
        event=SubscribeEvent.PROCESSING,
        queue=queue_to_domain(remote_response.queue),
        text_message=remote_response.content,
        message=remote_response.message,
    )
