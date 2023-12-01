package com.sector7.declarative_renderer

import android.opengl.GLES30.*
import com.sector7.math.Vec2i

internal class ImageObject private constructor(val handle: Int) {
    fun bind(slot: Int) {
        glActiveTexture(GL_TEXTURE0 + slot)
        glBindTexture(GL_TEXTURE_2D, handle)
    }

    companion object {
        fun new(dimensions: Vec2i): ImageObject {
            val image = intArrayOf(0).also { glGenTextures(1, it, 0) }[0]
            glBindTexture(GL_TEXTURE_2D, image)
            glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA8,
                dimensions.x,
                dimensions.y,
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                null
            )
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            return ImageObject(image)
        }
    }
}

internal class FramebufferObject private constructor(val handle: Int) {
    fun bind() = glBindFramebuffer(GL_FRAMEBUFFER, handle)

    companion object {
        val default = FramebufferObject(0)

        fun new(
            colorTargets: List<ImageObject>, depthStencilTarget: ImageObject?
        ): FramebufferObject {
            val framebuffer = intArrayOf(0).also { glGenFramebuffers(1, it, 0) }[0]
            assert(glGetError() == GL_NO_ERROR)
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
            assert(glGetError() == GL_NO_ERROR)
            colorTargets.forEachIndexed { i, image ->
                glFramebufferTexture2D(
                    GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + i, GL_TEXTURE_2D, image.handle, 0
                )
                assert(glGetError() == GL_NO_ERROR)
            }
            depthStencilTarget?.let {
                glFramebufferTexture2D(
                    GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, it.handle, 0
                )
                assert(glGetError() == GL_NO_ERROR)
            }
            return FramebufferObject(framebuffer)
        }
    }
}
