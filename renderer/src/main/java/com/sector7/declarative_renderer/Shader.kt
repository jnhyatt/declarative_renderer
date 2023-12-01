package com.sector7.declarative_renderer

import android.opengl.GLES30.*
import android.util.Log
import com.sector7.math.Mat4f
import com.sector7.math.Vec2i
import com.sector7.math.Vec3f

data class ShaderSource(val vertexShader: String, val fragmentShader: String)
data class UniformLocation(val shader: ShaderId, val name: String)

sealed class UniformValue
class Vec3fUniform(val value: Vec3f) : UniformValue()
class Mat4fUniform(val value: Mat4f) : UniformValue()
class ImageUniform(val slot: Int) : UniformValue()

data class UniformSet(val uniform: UniformId, val value: UniformValue)

internal class ShaderObject(val program: Int) {
    fun getUniform(name: String) = UniformObject(glGetUniformLocation(program, name))
    fun use() = glUseProgram(program)

    companion object {
        fun compile(source: ShaderSource): ShaderObject {
            val vShader = glCreateShader(GL_VERTEX_SHADER)
            glShaderSource(vShader, source.vertexShader)
            glCompileShader(vShader)
            Log.d("Shader", glGetShaderInfoLog(vShader))

            val fShader = glCreateShader(GL_FRAGMENT_SHADER)
            glShaderSource(fShader, source.fragmentShader)
            glCompileShader(fShader)
            Log.d("Shader", glGetShaderInfoLog(fShader))

            val program = glCreateProgram()
            glAttachShader(program, vShader)
            glAttachShader(program, fShader)
            glLinkProgram(program)
            glUseProgram(program)
            glDeleteShader(vShader)
            glDeleteShader(fShader)
            return ShaderObject(program)
        }
    }
}

internal class UniformObject(val location: Int) {
    fun setVec3f(x: Vec3f) = glUniform3f(location, x.x, x.y, x.z)
    fun setMat4f(x: Mat4f) = glUniformMatrix4fv(location, 1, false, x.asColumnMajorArray, 0)
    fun setImage(slot: Int) = glUniform1i(location, slot)
}

internal class ImageObject(val handle: Int) {
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
                GL_RGBA,
                dimensions.x,
                dimensions.y,
                0,
                GL_RGBA,
                GL_UNSIGNED_INT,
                null
            )
            return ImageObject(image)
        }
    }
}

internal class FramebufferObject(private val handle: Int) {
    fun bind() = glBindFramebuffer(GL_FRAMEBUFFER, handle)

    companion object {
        fun new(
            colorTargets: List<ImageObject>, depthStencilTarget: ImageObject?
        ): FramebufferObject {
            val framebuffer = intArrayOf(0).also { glGenFramebuffers(1, it, 0) }[0]
            colorTargets.forEachIndexed { i, image ->
                glFramebufferTexture2D(
                    GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + i, GL_TEXTURE_2D, image.handle, 0
                )
            }
            depthStencilTarget?.let {
                glFramebufferTexture2D(
                    GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, it.handle, 0
                )
            }
            return FramebufferObject(framebuffer)
        }
    }
}
