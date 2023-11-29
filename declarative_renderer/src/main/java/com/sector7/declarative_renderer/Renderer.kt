package com.sector7.declarative_renderer

import android.opengl.GLES30.*
import com.sector7.math.Vec2i

class Renderer {
    private class MeshDraw(val mesh: MeshId, val uniforms: List<UniformSet>, val pipeline: Pipeline)
    private class PendingShader(val id: ShaderId, val source: ShaderSource)
    private class PendingMesh(val id: MeshId, val mesh: Mesh)
    private class PendingUniform(val id: UniformId, val uniform: UniformLocation)

    private val shaderIdGenerator = ShaderId.Generator()
    private val meshIdGenerator = MeshId.Generator()
    private val uniformIdGenerator = UniformId.Generator()

    private val shaders = HashMap<ShaderId, ShaderObject>()
    private val uniforms = HashMap<UniformId, UniformObject>()
    private val meshes = HashMap<MeshId, MeshObject>()

    private val pendingShaders = mutableListOf<PendingShader>()
    private val pendingMeshes = mutableListOf<PendingMesh>()
    private val pendingUniforms = mutableListOf<PendingUniform>()

    private val vars = FuncVars().apply {
        floatVars[0] = 1f
        floatVars[1] = 1f
    }

    fun eval(f: FloatFunc) = with(vars) { f.eval() }

    fun newShader(source: ShaderSource) = synchronized(this) {
        shaderIdGenerator.next().also { id ->
            pendingShaders.add(PendingShader(id, source))
        }
    }

    fun newMesh(mesh: Mesh) = synchronized(this) {
        meshIdGenerator.next().also { id ->
            pendingMeshes.add(PendingMesh(id, mesh))
        }
    }

    fun clearMeshes() {
        // TODO This is no longer that important
    }

    fun getUniform(shader: ShaderId, name: String) = synchronized(this) {
        uniformIdGenerator.next().also { id ->
            pendingUniforms.add(PendingUniform(id, UniformLocation(shader, name)))
        }
    }

    fun drawFrame(drawList: List<DrawCommand>) {
        val draws = synchronized(this) {
            pendingShaders.forEach {
                shaders[it.id] = ShaderObject.compile(it.source)
            }
            pendingMeshes.forEach { meshes[it.id] = MeshObject.new(it.mesh) }
            pendingUniforms.forEach {
                uniforms[it.id] = shaders[it.uniform.shader]!!.getUniform(it.uniform.name)
            }
            pendingShaders.clear()
            pendingMeshes.clear()
            pendingUniforms.clear()
            drawList.groupBy { it.pipeline.shader }.mapValues {
                it.value.map { draw -> MeshDraw(draw.mesh, draw.uniforms, draw.pipeline) }
            }
        }

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        draws.keys.forEach { shaderId ->
            shaders[shaderId]!!.use()
            for (draw in draws[shaderId]!!) {
                // Set pipeline state
                glViewport(0, 0, draw.pipeline.viewport.x, draw.pipeline.viewport.y)
                glClearColor(0f, 0f, 0f, 1f)
                glLineWidth(4f)
                glEnable(GL_DEPTH_TEST)
                vars.floatVars[screenWidth.index] = draw.pipeline.viewport.x.toFloat()
                vars.floatVars[screenHeight.index] = draw.pipeline.viewport.y.toFloat()
                for (uniformSet in draw.uniforms) {
                    val uniform = uniforms[uniformSet.uniform]!!
                    with(vars) {
                        when (val value = uniformSet.value) {
                            is Vec3fUniform -> uniform.setVec3f(value.value.eval())
                            is Mat4fUniform -> uniform.setMat4f(value.value.eval())
                        }
                    }
                }
                meshes[draw.mesh]!!.draw()
            }
        }
    }

    companion object {
        val screenWidth = FloatVar(0)
        val screenHeight = FloatVar(1)
    }
}

data class Pipeline(val shader: ShaderId, val viewport: Vec2i)

data class DrawCommand(val pipeline: Pipeline, val mesh: MeshId, val uniforms: List<UniformSet>)

data class ShaderId(private val id: Int) {
    class Generator {
        private var id = 0
        fun next() = ShaderId(id++)
    }
}

data class MeshId(private val id: Int) {
    class Generator {
        private var id = 0
        fun next() = MeshId(id++)
    }
}

data class UniformId(private val id: Int) {
    class Generator {
        private var id = 0
        fun next() = UniformId(id++)
    }
}
