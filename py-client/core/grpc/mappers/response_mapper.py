from core.grpc.response import Response
from proto.io.github.vinicreis.pubsub.server.core.model.data.result_pb2 import Result
from proto.io.github.vinicreis.pubsub.server.core.model.response.publish_response_pb2 import PublishResponse


def publish_response_to_domain(remote_response: PublishResponse) -> Response:
    if remote_response == Result.SUCCESS:
        return Response(
            result=Response.Result.SUCCESS,
            data=remote_response.queue,
        )
    if remote_response == Result.ERROR:
        return Response(
            result=Response.Result.FAIL,
            data=None,
            error=remote_response.message,
        )

    raise ValueError(f"Unknown response: {remote_response}")
