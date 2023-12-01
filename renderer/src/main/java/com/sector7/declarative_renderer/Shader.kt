package com.sector7.declarative_renderer

import android.opengl.GLES30.GL_FRAGMENT_SHADER
import android.opengl.GLES30.GL_FRAMEBUFFER
import android.opengl.GLES30.GL_RGBA
import android.opengl.GLES30.GL_TEXTURE0
import android.opengl.GLES30.GL_TEXTURE_2D
import android.opengl.GLES30.GL_UNSIGNED_INT
import android.opengl.GLES30.GL_VERTEX_SHADER
import android.opengl.GLES30.glActiveTexture
import android.opengl.GLES30.glAttachShader
import android.opengl.GLES30.glBindFramebuffer
import android.opengl.GLES30.glBindTexture
import android.opengl.GLES30.glCompileShader
import android.opengl.GLES30.glCreateProgram
import android.opengl.GLES30.glCreateShader
import android.opengl.GLES30.glDeleteShader
import android.opengl.GLES30.glGenFramebuffers
import android.opengl.GLES30.glGenTextures
import android.opengl.GLES30.glGetShaderInfoLog
import android.opengl.GLES30.glGetUniformLocation
import android.opengl.GLES30.glLinkProgram
import android.opengl.GLES30.glShaderSource
import android.opengl.GLES30.glTexImage2D
import android.opengl.GLES30.glUniform1i
import android.opengl.GLES30.glUniform3f
import android.opengl.GLES30.glUniformMatrix4fv
import android.opengl.GLES30.glUseProgram
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

internal class ShaderObject(private val program: Int) {
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

internal class UniformObject(private val location: Int) {
    fun setVec3f(x: Vec3f) = glUniform3f(location, x.x, x.y, x.z)
    fun setMat4f(x: Mat4f) = glUniformMatrix4fv(location, 1, false, x.asColumnMajorArray, 0)
    fun setImage(slot: Int) = glUniform1i(location, slot)
}

internal class ImageObject(private val image: Int) {
    fun bind(slot: Int) {
        glActiveTexture(GL_TEXTURE0 + slot)
        glBindTexture(GL_TEXTURE_2D, image)
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

internal class FramebufferObject(private val framebuffer: Int) {
    fun bind() = glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)

    companion object {
        fun new() = FramebufferObject(intArrayOf(0).also { glGenFramebuffers(1, it, 0) }[0])
    }
}
