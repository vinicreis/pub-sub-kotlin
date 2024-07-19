package io.github.vinicreis.pubsub.server.channel.infra.service

import io.github.vinicreis.domain.model.ResultOuterClass
import io.github.vinicreis.domain.server.channel.request.AddRequest
import io.github.vinicreis.domain.server.channel.request.ListRequest
import io.github.vinicreis.domain.server.channel.request.PublishRequest
import io.github.vinicreis.domain.server.channel.request.RemoveByIdRequest
import io.github.vinicreis.domain.server.channel.request.SubscribeRequest
import io.github.vinicreis.domain.server.channel.response.AddResponse
import io.github.vinicreis.domain.server.channel.response.ListResponse
import io.github.vinicreis.domain.server.channel.response.PublishResponse
import io.github.vinicreis.domain.server.channel.response.RemoveByIdResponse
import io.github.vinicreis.domain.server.channel.response.SubscribeResponse
import io.github.vinicreis.domain.server.channel.response.addResponse
import io.github.vinicreis.domain.server.channel.response.listResponse
import io.github.vinicreis.domain.server.channel.response.removeByIdResponse
import io.github.vinicreis.domain.server.channel.service.ChannelServiceGrpcKt
import io.github.vinicreis.pubsub.server.channel.domain.mapper.asDomain
import io.github.vinicreis.pubsub.server.channel.domain.mapper.asRemote
import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.pubsub.server.channel.domain.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.channel.domain.service.ChannelService
import io.grpc.BindableService
import io.grpc.kotlin.ClientCalls
import io.grpc.stub.ClientCallStreamObserver
import kotlinx.coroutines.flow.Flow
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class ChannelServiceGRPC(
    coroutineContext: CoroutineContext,
    private val channelRepository: ChannelRepository,
    private val logger: Logger = Logger.getLogger("ChannelService"),
) : ChannelService, ChannelServiceGrpcKt.ChannelServiceCoroutineImplBase(coroutineContext) {


    override suspend fun add(request: AddRequest): AddResponse {
        return try {
            Channel(
                id = request.id,
                type = request.type.asDomain,
                name = request.name ?: request.id,
            ).let { channel ->
                when(val result = channelRepository.add(channel)) {
                    is ChannelRepository.Result.Add.Error -> addResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.message
                    }

                    is ChannelRepository.Result.Add.AlreadyFound -> addResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Channel ${request.id} already exists"
                        this.channel = result.channel.asRemote
                    }

                    is ChannelRepository.Result.Add.Success -> addResponse {
                        this.result = ResultOuterClass.Result.SUCCESS
                        this.channel = result.channel.asRemote
                    }
                }
            }
        } catch (t: Throwable) {
            logger.severe("Failed to process add request: $t")
            addResponse {
                result = ResultOuterClass.Result.ERROR
                message = "Something went wrong..."
            }
        }
    }

    override suspend fun list(request: ListRequest): ListResponse {
        return try {
            channelRepository.getAll().let { result ->
                when(result) {
                    is ChannelRepository.Result.GetAll.Success -> listResponse {
                        this.result = ResultOuterClass.Result.SUCCESS
                        result.channels.forEach { channel -> channels.add(channel.asRemote) }
                    }

                    is ChannelRepository.Result.GetAll.Error -> listResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        this.message = result.message
                    }
                }
            }
        } catch (t: Throwable) {
            logger.severe("Failed to process add request: $t")
            listResponse {
                result = ResultOuterClass.Result.ERROR
            }
        }
    }

    override suspend fun removeById(request: RemoveByIdRequest): RemoveByIdResponse {
        return try {
            channelRepository.removeById(request.id).let { result ->
                when(result) {
                    ChannelRepository.Result.Remove.NotFound -> removeByIdResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Channel ${request.id} not found"
                    }

                    is ChannelRepository.Result.Remove.Error -> removeByIdResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.message
                    }

                    is ChannelRepository.Result.Remove.Success -> removeByIdResponse {
                        this.result = ResultOuterClass.Result.SUCCESS
                        id = request.id
                        this.channel = result.channel.asRemote
                    }
                }
            }
        } catch (t: Throwable) {
            logger.severe("Failed to proccess remove by id request: $t")
            removeByIdResponse {
                result = ResultOuterClass.Result.ERROR
            }
        }
    }

    override suspend fun publish(request: PublishRequest): PublishResponse {
        TODO("Not yet implemented")
    }

    override fun subscribe(request: SubscribeRequest): Flow<SubscribeResponse> {
        TODO("Not yet implemented")
    }
}
