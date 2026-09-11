package app.notetaker.organization

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

interface TextEmbedding : AutoCloseable {
    val version: String
    fun embed(text: String): FloatArray
    override fun close() {}
}

/** Construct and use on a worker thread. The bundled model never sends text off device. */
class LocalTextEmbedding(context: Context) : TextEmbedding {
    override val version = "minilm-qint8-arm64-v1-wordpiece-v1"
    private val environment = ai.onnxruntime.OrtEnvironment.getEnvironment()
    private val tokenizer = WordPieceTokenizer(context.assets.open("minilm-vocab.txt").bufferedReader().use { it.readLines() })
    private val session = ai.onnxruntime.OrtSession.SessionOptions().use { options ->
        options.setIntraOpNumThreads(2)
        options.setInterOpNumThreads(1)
        environment.createSession(context.assets.open("minilm.onnx").use { it.readBytes() }, options)
    }
    override fun embed(text: String): FloatArray {
        val pieces = tokenizer.tokenize(text)
        if (pieces.isEmpty()) return FloatArray(384)
        val sum = FloatArray(384)
        pieces.chunked(254).forEach { chunk ->
            val ids = longArrayOf(101) + chunk.toLongArray() + longArrayOf(102)
            ai.onnxruntime.OnnxTensor.createTensor(environment, arrayOf(ids)).use { input ->
                ai.onnxruntime.OnnxTensor.createTensor(environment, arrayOf(LongArray(ids.size) { 1 })).use { mask ->
                    ai.onnxruntime.OnnxTensor.createTensor(environment, arrayOf(LongArray(ids.size))).use { types ->
                        session.run(mapOf("input_ids" to input, "attention_mask" to mask, "token_type_ids" to types)).use { result ->
                            @Suppress("UNCHECKED_CAST")
                            val tokens = (result[0].value as Array<Array<FloatArray>>)[0]
                            val pooled = FloatArray(384)
                            tokens.forEach { vector -> vector.indices.forEach { pooled[it] += vector[it] / tokens.size } }
                            val normalized = normalize(pooled)
                            sum.indices.forEach { sum[it] += normalized[it] * chunk.size }
                        }
                    }
                }
            }
        }
        return normalize(sum)
    }
    override fun close() = session.close()
}

fun normalize(values: FloatArray): FloatArray {
    val norm = sqrt(values.sumOf { it.toDouble() * it })
    return if (norm == 0.0) values else FloatArray(values.size) { (values[it] / norm).toFloat() }
}
fun similarity(a: FloatArray, b: FloatArray): Double =
    if (a.size != b.size) 0.0 else a.indices.sumOf { a[it].toDouble() * b[it] }.coerceIn(-1.0, 1.0)
fun FloatArray.bytes(): ByteArray = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN).also { b -> forEach(b::putFloat) }.array()
fun ByteArray.vector(): FloatArray = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().let { b -> FloatArray(b.remaining()).also(b::get) }
