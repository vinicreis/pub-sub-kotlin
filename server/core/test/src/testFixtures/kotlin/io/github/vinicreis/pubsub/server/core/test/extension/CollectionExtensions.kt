package io.github.vinicreis.pubsub.server.core.test.extension

import kotlin.random.Random

fun <E> Collection<E>.randomItem(): E = takeIf { it.isNotEmpty() }?.elementAt(Random.nextInt(size))
    ?: error("List can not be empty to get a random element")

fun <E> Collection<E>.randomSlice(): List<E> = takeIf { it.isNotEmpty() }?.let {
    buildList { repeat(Random.nextInt(this@randomSlice.size)) { add(this@randomSlice.randomItem()) } }
} ?: error("List can not be empty to get a random element")
