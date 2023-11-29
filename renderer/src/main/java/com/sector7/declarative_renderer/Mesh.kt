package com.sector7.declarative_renderer

import com.sector7.math.Vec3f

data class IndexTriangle(val a: Int, val b: Int, val c: Int)
data class IndexLine(val a: Int, val b: Int)

sealed class Mesh
data class IndexedLineMesh(val vertices: List<Vec3f>, val lines: List<IndexLine>) : Mesh()
data class IndexedTriMesh(val vertices: List<Vec3f>, val triangles: List<IndexTriangle>) : Mesh()
