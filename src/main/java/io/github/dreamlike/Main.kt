package io.github.dreamlike

import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.annotations.QuarkusMain


@QuarkusMain
class Main

fun main(vararg args: String) {
    Quarkus.run(*args)
}

