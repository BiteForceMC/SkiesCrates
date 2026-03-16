package com.dawnshade.biteforce.bitecrates.util

import kotlin.random.Random

class RandomCollection<E> @JvmOverloads constructor(private val random: Random = Random) {
    private val map = mutableMapOf<E, Double>()
    private var totalWeight = 0.0

    fun add(element: E, weight: Double) {
        require(weight > 0) { "Weight must be positive" }
        val currentWeight = map[element] ?: 0.0
        map[element] = currentWeight + weight
        totalWeight += weight
    }

    fun remove(element: E): Boolean {
        val weight = map.remove(element)
        if (weight != null) {
            totalWeight -= weight
            return true
        }
        return false
    }

    fun decrement(element: E, weight: Double = 1.0): Boolean {
        val current = map[element] ?: return false
        val newWeight = current - weight
        return if (newWeight > 0) {
            map[element] = newWeight
            totalWeight -= weight
            true
        } else {
            map.remove(element)
            totalWeight -= current
            true
        }
    }

    fun next(): E? {
        if (map.isEmpty()) return null
        var r = random.nextDouble() * totalWeight
        for ((element, weight) in map) {
            r -= weight
            if (r <= 0.0) {
                return element
            }
        }
        return map.keys.last() 
    }

    fun entries(): Map<E, Double> = map.toMap()

    fun copy(): RandomCollection<E> {
        val collection = RandomCollection<E>(random)
        map.forEach { (element, weight) ->
            collection.add(element, weight)
        }
        return collection
    }

    fun size(): Int = map.size

    fun isEmpty(): Boolean = map.isEmpty()
}
