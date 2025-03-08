package com.zachm.employeelogin.util

import kotlin.math.abs
import kotlin.math.sqrt

data class Embedding (val embeddings: FloatArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Embedding

        return embeddings.contentEquals(other.embeddings)
    }

    override fun hashCode(): Int {
        return embeddings.contentHashCode()
    }

    fun compareSigmoid(other: Embedding): Float {
        return 0f
    }

    fun compareDistance(other: Embedding): Float {
        if(embeddings.size != other.embeddings.size) return 0f

        var distance = 0f

        for(i in embeddings.indices) {
            val difference = embeddings[i] - other.embeddings[i]
            distance += difference * difference
        }

        return 1f - (sqrt(distance) / sqrt(4f * embeddings.size))
    }

    /**
     * Manhattan distance normalized between 0f - 1f
     */
    fun compareManhattanDistance(other: Embedding): Float {
        if(embeddings.size != other.embeddings.size) return 0f

        var distance = 0f

        for(i in embeddings.indices) {
            distance += abs(embeddings[i] - other.embeddings[i])
        }

        return 1f - (distance / (embeddings.size*2))
    }
}