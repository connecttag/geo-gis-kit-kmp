package org.connecttag.lib.gis.android

import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

public object KmlInput {
    public fun read(input: InputStream, sourceName: String): String {
        val limited = LimitInputStream(input, 64L * 1024 * 1024)
        val buffered = limited.buffered()
        return buffered.use { source ->
            source.mark(4)
            val first = source.read()
            val second = source.read()
            source.reset()
            if (sourceName.endsWith(".kmz", ignoreCase = true) || (first == 0x50 && second == 0x4b)) {
                ZipInputStream(source).use archive@ { zip ->
                    var entries = 0
                    var expanded = 0L
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        require(++entries <= 128) { "KMZ_ENTRY_LIMIT" }
                        val path = entry.name.replace('\\', '/')
                        require(!path.startsWith('/') && ':' !in path && path.split('/').none { it == ".." }) { "KMZ_ENTRY_INVALID" }
                        val selected = !entry.isDirectory && path.endsWith(".kml", ignoreCase = true)
                        val content = if (selected) ByteArrayOutputStream() else null
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            expanded += count
                            require(expanded <= 256L * 1024 * 1024) { "KMZ_EXPANDED_LIMIT" }
                            if (content != null) {
                                require(content.size().toLong() + count <= 32L * 1024 * 1024) { "KML_INPUT_LIMIT" }
                                content.write(buffer, 0, count)
                            }
                        }
                        if (content != null) return@archive content.toByteArray().decodeToString(throwOnInvalidSequence = true)
                        zip.closeEntry()
                    }
                    error("KMZ_KML_MISSING")
                }
            } else {
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    require(output.size().toLong() + count <= 32L * 1024 * 1024) { "KML_INPUT_LIMIT" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray().decodeToString(throwOnInvalidSequence = true)
            }
        }
    }
}
private class LimitInputStream(input: InputStream, private val limit: Long) : FilterInputStream(input) {
    private var count: Long = 0
    private fun add(size: Int) { if (size > 0) { count += size; require(count <= limit) { "KML_COMPRESSED_LIMIT" } } }
    override fun read(): Int = `in`.read().also { if (it >= 0) add(1) }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = `in`.read(buffer, offset, length).also { add(it) }
}
