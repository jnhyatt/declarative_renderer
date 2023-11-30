package com.sector7.declarative_renderer

import android.opengl.GLES30.*
import android.util.Log
import com.sector7.math.Mat4f
import com.sector7.math.Vec3f

data class ShaderSource(val vertexShader: String, val fragmentShader: String)
data class UniformLocation(val shader: ShaderId, val name: String)

sealed class UniformValue
class Vec3fUniform(val value: Vec3f) : UniformValue()
class Mat4fUniform(val value: Mat4f) : UniformValue()

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
}
