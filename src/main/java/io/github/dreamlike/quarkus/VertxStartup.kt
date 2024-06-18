package io.github.dreamlike.quarkus

import io.github.dreamlike.suit.WebVerticleCase
import io.quarkus.arc.InstanceHandle
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import io.vertx.core.Vertx
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Instance


@ApplicationScoped
class VertxStartup (val vertx: Vertx, val verticleGetter: Instance<WebVerticleCase>){

    fun onStart(@Observes ev: StartupEvent) {
        for (i in 1..5) {
            vertx.deployVerticle(verticleGetter.get())
        }
    }

    fun onStop(@Observes ev: ShutdownEvent) {
        vertx.close()
    }
}