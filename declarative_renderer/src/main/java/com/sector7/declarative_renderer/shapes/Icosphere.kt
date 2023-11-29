package com.sector7.declarative_renderer.shapes

import com.sector7.declarative_renderer.IndexLine
import com.sector7.declarative_renderer.IndexTriangle
import com.sector7.declarative_renderer.IndexedTriMesh
import com.sector7.math.Vec3f
import kotlin.math.*

fun icosphere(subdivisions: Int): IndexedTriMesh {
    val vertices = sequenceOf(
        Vec3f(0.0f, 1.0f, 1.618f),
        Vec3f(0.0f, 1.0f, -1.618f),
        Vec3f(0.0f, -1.0f, 1.618f),
        Vec3f(0.0f, -1.0f, -1.618f),
        Vec3f(1.0f, 1.618f, 0.0f),
        Vec3f(1.0f, -1.618f, 0.0f),
        Vec3f(-1.0f, 1.618f, 0.0f),
        Vec3f(-1.0f, -1.618f, 0.0f),
        Vec3f(1.618f, 0.0f, 1.0f),
        Vec3f(1.618f, 0.0f, -1.0f),
        Vec3f(-1.618f, 0.0f, 1.0f),
        Vec3f(-1.618f, 0.0f, -1.0f),
    ).map { it.normalized }.toMutableList()

    var indices = listOf(
        IndexTriangle(0, 2, 8),
        IndexTriangle(0, 2, 10),
        IndexTriangle(0, 4, 6),
        IndexTriangle(0, 4, 8),
        IndexTriangle(0, 6, 10),
        IndexTriangle(1, 3, 9),
        IndexTriangle(1, 3, 11),
        IndexTriangle(1, 4, 6),
        IndexTriangle(1, 4, 9),
        IndexTriangle(1, 6, 11),
        IndexTriangle(2, 5, 7),
        IndexTriangle(2, 5, 8),
        IndexTriangle(2, 7, 10),
        IndexTriangle(3, 5, 7),
        IndexTriangle(3, 5, 9),
        IndexTriangle(3, 7, 11),
        IndexTriangle(4, 8, 9),
        IndexTriangle(5, 8, 9),
        IndexTriangle(6, 10, 11),
        IndexTriangle(7, 10, 11),
    )

    val subdivide = {
        val newIndices = mutableListOf<IndexTriangle>()
        val midpoints = HashMap<IndexLine, Int>()

        val getVertex = { x: Int, y: Int ->
            val key = IndexLine(min(x, y), max(x, y))
            val result = midpoints[key]
            if (result == null) {
                val newResult = vertices.size
                vertices.add((vertices[x] + vertices[y]).normalized)
                midpoints[key] = newResult
                newResult
            } else result
        }

        for (tri in indices) {
            val mid = arrayOf(
                getVertex(tri.a, tri.b),
                getVertex(tri.b, tri.c),
                getVertex(tri.c, tri.a),
            )
            newIndices.add(IndexTriangle(tri.a, mid[0], mid[2]))
            newIndices.add(IndexTriangle(tri.b, mid[1], mid[0]))
            newIndices.add(IndexTriangle(tri.c, mid[2], mid[1]))
            newIndices.add(IndexTriangle(mid[0], mid[1], mid[2]))
        }

        newIndices
    }

    repeat(subdivisions) { indices = subdivide() }

    return IndexedTriMesh(vertices, indices)
}
