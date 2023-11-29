package com.sector7.declarative_renderer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

internal fun IntArray.allocateBuffer(): IntBuffer =
    ByteBuffer.allocateDirect(size * Int.SIZE_BYTES).order(ByteOrder.nativeOrder()).asIntBuffer()
        .also { it.put(this).position(0) }

internal fun FloatArray.allocateBuffer(): FloatBuffer =
    ByteBuffer.allocateDirect(size * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
        .asFloatBuffer().also { it.put(this).position(0) }
