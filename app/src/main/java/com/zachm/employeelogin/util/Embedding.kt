package com.zachm.employeelogin.util

import kotlin.math.abs

data class Embedding (private val embeddings: FloatArray) {
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
            distance += abs(embeddings[i] - other.embeddings[i])
        }

        distance /= embeddings.size

        return distance
    }
}