package io.github.dreamlike

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisURI
import io.lettuce.core.api.coroutines
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisAPI
import io.vertx.redis.client.RedisOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.net.http.HttpResponse.BodyHandlers

fun main() {
    DatabindCodec.mapper().registerKotlinModule()
    val invokeExact = NativeCase.pageSizeMH.invokeExact() as Int
    println("current page size: $invokeExact")

    val vertx = Vertx.vertx()


    vertx.deployVerticle(WebVerticle())
        .andThen {
            if (it.failed()) {
                it.cause().printStackTrace()
            }
        }
}

class WebVerticle : CoroutineVerticle() {
    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override suspend fun start() {
        val router = Router.router(vertx)
        val dnsClient = vertx.createDnsClient()
        println("start redis")
        val redisConnect = vertx.createRedisClient()
            .connectAsync(StringCodec.UTF8,
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
                    json {
                        obj(
                            "message" to "Hello $name, you are $age years old.",
                            "person_info" to obj(
                                "name" to name,
                                "age" to age
                            )
                        )
                    }
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
                println(getHAll.javaClass)
                redisConnect.del("key")
                redisConnect.del("hashKey")
                redisConnect.lpush("lpush", "123123")
                val pop = redisConnect.rpop("lpush")

                it.json(
                    json {
                        obj(
                            "get" to getRes,
                            "getHAll" to getHAll,
                            "pop" to pop
                        )
                    }
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



fun Route.coroutineHandler(handle: suspend (RoutingContext) -> Unit) {
    handler { ctx ->
        CoroutineScope(ctx.vertx().dispatcher()).launch {
            handle(ctx)
        }
    }
}
