package io.github.dreamlike.suit

import io.github.dreamlike.entity.Person
import io.lettuce.core.RedisURI
import io.lettuce.core.api.coroutines
import io.lettuce.core.codec.StringCodec
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

import jakarta.enterprise.context.Dependent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Dependent
class WebVerticleCase : AbstractVerticle() {

    override fun start() {
        val router = Router.router(vertx)
        val dnsClient = vertx.createDnsClient()
        println("start redis")
        CoroutineScope(vertx.dispatcher()).launch {
            val redisConnect = vertx.createRedisClient()
                .connectAsync(
                    StringCodec.UTF8,
                    RedisURI.create("redis://dreamlike@localhost:6379")
                )
                .await()
                .coroutines()
            println("end redis")
            router.get("/delay")
                .coroutineHandler {
                    delay(1000)
                    it.end("Hello World! ${Thread.currentThread()}")
                }

            router.get("/pageSize")
                .coroutineHandler {
                    val pageSize = FFICase.pageSizeMH.invokeExact() as Int
                    it.end("PageSize: $pageSize")
                }

            router.get("/param")
                .coroutineHandler {
                    val name = it.queryParam("name")?.firstOrNull() ?: "dreamlike"
                    it.json(
                        Person(name, 123)
                    )
                }
            router.get("/dns")
                .coroutineHandler {
                    val result = dnsClient.resolveA("www.baidu.com").coAwait()
                    it.end("DNS: $result")
                }
            router.post("/post")
                .handler(BodyHandler.create())
                .coroutineHandler {
                    val (name, age) = it.body().asPojo(Person::class.java)
                    it.json(
                       mapOf(
                            "message" to "Hello $name, you are $age years old.",
                            "person_info" to mapOf(
                                 "name" to name,
                                 "age" to age
                            )
                       )
                    )
                }

            router.get("/redis")
                .coroutineHandler {
                    redisConnect.set("key", "value")
                    val getRes = redisConnect.get("key")

                    redisConnect.hmset("hashKey", mapOf("field1" to "value1", "field2" to "value2"))

                    val getHAll = redisConnect.hgetall("hashKey")
                        .toList()
                        .map {
                            it.key.toString() to it.value.toString()
                        }
                    redisConnect.del("key")
                    redisConnect.del("hashKey")
                    redisConnect.lpush("lpush", "123123")
                    val pop = redisConnect.rpop("lpush")
                    it.json(
                        mapOf(
                            "get" to getRes,
                            "getHAll" to getHAll,
                            "pop" to pop
                        )
                    )
                }


            try {
                println("start server")
                val httpServer = vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(4399).coAwait()
                println("Server started at http://localhost:4399 $httpServer")
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }



    }
}



fun Route.coroutineHandler(handle: suspend (RoutingContext) -> Unit) {
    handler { ctx ->
        CoroutineScope(ctx.vertx().dispatcher()).launch {
            handle(ctx)
        }
    }
}

fun Vertx.dispatcher() = Executor { r -> this.orCreateContext.runOnContext { r.run() } }.asCoroutineDispatcher()


suspend fun <T> Future<T>.coAwait() = suspendCancellableCoroutine<T> { c ->
    this.onSuccess { c.resume(it) }
    this.onFailure { c.resumeWithException(it) }
}