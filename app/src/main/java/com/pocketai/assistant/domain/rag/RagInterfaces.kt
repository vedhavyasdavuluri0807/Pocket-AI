package com.pocketai.assistant.domain.rag

/**
 * Extension interfaces for future on-device RAG (Retrieval-Augmented Generation).
 * Designed for local embedding execution, chunking, and local vector search.
 */

data class DocumentChunk(
    val chunkId: String,
    val documentUri: String,
    val title: String,
    val text: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val metadata: Map<String, String> = emptyMap()
)

data class EmbeddingVector(
    val dimensions: Int,
    val values: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingVector) return false
        return dimensions == other.dimensions && values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        var result = dimensions
        result = 31 * result + values.contentHashCode()
        return result
    }
}

data class RetrievalMatch(
    val chunk: DocumentChunk,
    val score: Float,
    val citationText: String
)

interface DocumentChunker {
    fun chunkDocument(documentText: String, documentUri: String, title: String, maxChunkSize: Int = 500): List<DocumentChunk>
}

interface LocalEmbeddingModel {
    val isModelLoaded: Boolean
    val vectorDimension: Int
    suspend fun generateEmbedding(text: String): EmbeddingVector
    suspend fun generateBatchEmbeddings(texts: List<String>): List<EmbeddingVector>
}

interface LocalVectorIndex {
    suspend fun indexChunks(chunks: List<Pair<DocumentChunk, EmbeddingVector>>)
    suspend fun query(queryVector: EmbeddingVector, topK: Int = 3, minScore: Float = 0.65f): List<RetrievalMatch>
    suspend fun deleteDocumentIndex(documentUri: String)
    suspend fun clearIndex()
}

interface LocalRagRetriever {
    suspend fun retrieveRelevantContext(query: String, maxResults: Int = 3): List<RetrievalMatch>
}

