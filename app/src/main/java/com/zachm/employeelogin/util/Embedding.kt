package com.zachm.employeelogin.util

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

data class Embedding (var embeddings: FloatArray) {

    /**
     * Normalize the embedding from (-1f, 1f) to (0f, 1f)
     */
    fun normalize(embedding: FloatArray): FloatArray {
        var norm = 0f
        for (v in embedding) norm += v * v
        norm = sqrt(norm)

        return if (norm > 0) embedding.map { it / norm }.toFloatArray() else embedding
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

    /**
     * Cosine similarity normalized between 0f - 1f.
     * Dot Product (a * b) / (sqrt(a^2) * sqrt(b^2))
     */
    fun compareCosineSimilarity(other: Embedding): Float {
        var dotProduct = 0f
        var normA = 0f //This Embedding (A)
        var normB = 0f //Compared Embedding (B)

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

    /**
     * Euclidean distance normalized between 0f - 1f.
     * sqrt((a - b)^2)
     */
    fun compareEuclideanDistance(other: Embedding): Float {
        val thisEmbedding = normalize(embeddings)
        val otherEmbedding = normalize(other.embeddings)

        if(embeddings.size != other.embeddings.size) return 0f

        var distance = 0f

        for(i in embeddings.indices) {
            val difference = thisEmbedding[i] - otherEmbedding[i]
            distance += difference * difference
        }

        //4f comes from the distance of -1 to 1 and then sqrt.
        return 1f - (sqrt(distance) / (sqrt(4f * embeddings.size)))
    }

    /**
     * Manhattan distance normalized between 0f - 1f.
     * abs(a - b)
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