package io.github.dreamlike.suit

import io.lettuce.core.RedisClient
import io.lettuce.core.resource.DefaultClientResources
import io.lettuce.core.resource.EventLoopGroupProvider
import io.netty.channel.EventLoopGroup
import io.netty.util.concurrent.DefaultPromise
import io.netty.util.concurrent.EventExecutorGroup
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GlobalEventExecutor
import io.vertx.core.Vertx
import io.vertx.core.impl.ContextInternal
import io.vertx.core.net.impl.VertxEventLoopGroup
import java.util.concurrent.TimeUnit

fun Vertx.createRedisClient() :RedisClient {
    val contextInternal = this.orCreateContext as ContextInternal

    val currentEventLoop = VertxEventLoopGroup().apply {
        addWorker(contextInternal.nettyEventLoop())
    }

    val resources = DefaultClientResources
        .builder()
        .eventLoopGroupProvider(EventLoopGroupProvider(currentEventLoop))
        .build()

   return RedisClient.create(resources)
}

private class EventLoopGroupProvider(private val eventLoopGroup: VertxEventLoopGroup) :EventLoopGroupProvider {
    override fun <T : EventLoopGroup?> allocate(type: Class<T>?): T {
        return eventLoopGroup as T
    }

    override fun threadPoolSize() = 1

    override fun release(
        eventLoopGroup: EventExecutorGroup,
        quietPeriod: Long,
        timeout: Long,
        unit: TimeUnit?
    ): Future<Boolean> {
        val promise = DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE)
        promise.setSuccess(true)
        return promise
    }

    override fun shutdown(quietPeriod: Long, timeout: Long, timeUnit: TimeUnit?): Future<Boolean> {
        val promise = DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE)
        promise.setSuccess(true)
        return promise
    }

}

