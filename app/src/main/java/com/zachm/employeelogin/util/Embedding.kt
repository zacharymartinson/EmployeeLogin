package com.zachm.employeelogin.util

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

data class Embedding (var embeddings: FloatArray) {

    fun normalize(vector: FloatArray): FloatArray {
        var norm = 0f
        for (v in vector) norm += v * v
        norm = sqrt(norm)

        return if (norm > 0) vector.map { it / norm }.toFloatArray() else vector
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Embedding

        return embeddings.contentEquals(other.embeddings)
    }

    override fun hashCode(): Int {
        return embeddings.contentHashCode()
    }

    fun compareCosineSimilarity(other: Embedding): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in embeddings.indices) {
            dotProduct += embeddings[i] * other.embeddings[i]
            normA += embeddings[i] * embeddings[i]
            normB += other.embeddings[i] * other.embeddings[i]
        }

        normA = sqrt(normA.toDouble()).toFloat()
        normB = sqrt(normB.toDouble()).toFloat()

        val cosineSimilarity = dotProduct / (normA * normB)

        return cosineSimilarity
    }

    fun compareSigmoid(other: Embedding): Float {
        return 0f
    }

    fun compareEuclideanDistance(other: Embedding): Float {
        if(embeddings.size != other.embeddings.size) return 0f

        var distance = 0f

        for(i in embeddings.indices) {
            val difference = embeddings[i] - other.embeddings[i]
            distance += difference * difference
        }
        val raw = sqrt(distance) / (sqrt(4f * embeddings.size))

        Log.d("CameraViewModel", "Raw: $raw")

        //4f comes from the distance of -1 to 1 and then sqrt. We then flip 1f to be most similar.
        return 1f - (sqrt(distance) / (sqrt(4f * embeddings.size)))
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