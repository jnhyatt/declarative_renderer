package com.sector7.declarative_renderer

import android.opengl.GLES30.*
import java.nio.FloatBuffer
import java.nio.IntBuffer

internal class MeshObject private constructor(
    val vao: Int, val vertexCount: Int, val prim: Int
) {
    fun draw() {
        glBindVertexArray(vao)
        glDrawElements(prim, vertexCount, GL_UNSIGNED_INT, 0)
    }

    companion object {
        fun new(mesh: Mesh) = when (mesh) {
            is IndexedLineMesh -> fromLineMesh(mesh)
            is IndexedTriMesh -> fromTriangleMesh(mesh)
        }

        private fun fromLineMesh(mesh: IndexedLineMesh): MeshObject {
            val vertices = mesh.vertices.flatMap { listOf(it.x, it.y, it.z) }.toFloatArray()
            val vertexBuffer = vertices.allocateBuffer()

            val indices = mesh.lines.flatMap { listOf(it.a, it.b) }.toIntArray()
            val indexBuffer = indices.allocateBuffer()

            return fromBuffers(vertexBuffer, indexBuffer, GL_LINES)
        }

        private fun fromTriangleMesh(mesh: IndexedTriMesh): MeshObject {
            val vertices = mesh.vertices.flatMap { listOf(it.x, it.y, it.z) }.toFloatArray()
            val vertexBuffer = vertices.allocateBuffer()

            val indices = mesh.triangles.flatMap { listOf(it.a, it.b, it.c) }.toIntArray()
            val indexBuffer = indices.allocateBuffer()

            return fromBuffers(vertexBuffer, indexBuffer, GL_TRIANGLES)
        }

        private fun fromBuffers(
            vertexBuffer: FloatBuffer, indexBuffer: IntBuffer, prim: Int
        ): MeshObject {
            val vao = intArrayOf(0).also { glGenVertexArrays(1, it, 0) }[0]
            val vbo = intArrayOf(0).also { glGenBuffers(1, it, 0) }[0]
            val ebo = intArrayOf(0).also { glGenBuffers(1, it, 0) }[0]

            glBindVertexArray(vao)

            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            val vertexBufferSize = vertexBuffer.limit() * Float.SIZE_BYTES
            glBufferData(
                GL_ARRAY_BUFFER, vertexBufferSize, vertexBuffer, GL_STATIC_DRAW
            )

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
            val indexBufferSize = indexBuffer.limit() * Int.SIZE_BYTES
            glBufferData(
                GL_ELEMENT_ARRAY_BUFFER, indexBufferSize, indexBuffer, GL_STATIC_DRAW
            )

            glEnableVertexAttribArray(0)
            glVertexAttribPointer(0, 3, GL_FLOAT, false, Float.SIZE_BYTES * 3, 0)

            return MeshObject(vao, indexBuffer.limit(), prim)
        }
    }
}
