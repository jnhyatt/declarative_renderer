package com.sector7.declarative_renderer

import android.opengl.GLES30.*
import com.sector7.math.Vec2i

class Renderer {
    private class MeshDraw(val mesh: MeshId, val uniforms: List<UniformSet>, val pipeline: Pipeline)
    private class PendingShader(val id: ShaderId, val source: ShaderSource)
    private class PendingMesh(val id: MeshId, val mesh: Mesh)
    private class PendingImage(val id: ImageId, val dimensions: Vec2i)
    private class PendingUniform(val id: UniformId, val uniform: UniformLocation)
    private class PendingFramebuffer(
        val id: FramebufferId, val colorTargets: List<ImageId>, val depthStencilTarget: ImageId?
    )

    private val shaderIdGenerator = ShaderId.Generator()
    private val meshIdGenerator = MeshId.Generator()
    private val uniformIdGenerator = UniformId.Generator()
    private val imageIdGenerator = ImageId.Generator()
    private val framebufferIdGenerator = FramebufferId.Generator()

    private val shaders = HashMap<ShaderId, ShaderObject>()
    private val uniforms = HashMap<UniformId, UniformObject>()
    private val meshes = HashMap<MeshId, MeshObject>()
    private val images = HashMap<ImageId, ImageObject>()
    private val framebuffers = HashMap<FramebufferId, FramebufferObject>()

    private val pendingShaders = mutableListOf<PendingShader>()
    private val pendingMeshes = mutableListOf<PendingMesh>()
    private val pendingUniforms = mutableListOf<PendingUniform>()
    private val pendingImages = mutableListOf<PendingImage>()
    private val pendingFramebuffers = mutableListOf<PendingFramebuffer>()

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

    fun newImage(dimensions: Vec2i) = synchronized(this) {
        imageIdGenerator.next().also { id ->
            pendingImages.add(PendingImage(id, dimensions))
        }
    }

    fun newFramebuffer(colorTargets: List<ImageId>, depthStencilTarget: ImageId?) =
        synchronized(this) {
            framebufferIdGenerator.next().also { id ->
                pendingFramebuffers.add(PendingFramebuffer(id, colorTargets, depthStencilTarget))
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

    fun setPipelineState(pipeline: Pipeline) {
        glViewport(0, 0, pipeline.viewport.x, pipeline.viewport.y)
        glClearColor(0f, 0f, 0f, 1f)
        glLineWidth(4f)
        glEnable(GL_DEPTH_TEST)
        pipeline.textureSlots.forEach { (slot, image) -> images[image]!!.bind(slot) }
    }

    fun executePass(pass: RenderPass) {
        // Sort draws by shader to minimize shader binds
        val draws = pass.draws.groupBy { it.pipeline.shader }.mapValues {
            it.value.map { draw -> MeshDraw(draw.mesh, draw.uniforms, draw.pipeline) }
        }

        framebuffers[pass.framebuffer]!!.bind()
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        for (shaderId in draws.keys) {
            shaders[shaderId]!!.use()
            for (draw in draws[shaderId]!!) {
                setPipelineState(draw.pipeline)
                for (uniformSet in draw.uniforms) {
                    val uniform = uniforms[uniformSet.uniform]!!
                    when (val value = uniformSet.value) {
                        is Vec3fUniform -> uniform.setVec3f(value.value)
                        is Mat4fUniform -> uniform.setMat4f(value.value)
                        is ImageUniform -> uniform.setImage(value.slot)
                    }
                }
                meshes[draw.mesh]!!.draw()
            }
        }
    }

    fun drawFrame(passes: List<RenderPass>) {
        synchronized(this) {
            pendingShaders.forEach {
                shaders[it.id] = ShaderObject.compile(it.source)
            }
            pendingMeshes.forEach { meshes[it.id] = MeshObject.new(it.mesh) }
            pendingUniforms.forEach {
                uniforms[it.id] = shaders[it.uniform.shader]!!.getUniform(it.uniform.name)
            }
            pendingImages.forEach { images[it.id] = ImageObject.new(it.dimensions) }
            pendingFramebuffers.forEach {
                framebuffers[it.id] = FramebufferObject.new(
                    it.colorTargets.map { id -> images[id]!! },
                    it.depthStencilTarget?.let { id -> images[id]!! },
                )
            }
            pendingShaders.clear()
            pendingMeshes.clear()
            pendingUniforms.clear()
            pendingImages.clear()
            pendingFramebuffers.clear()
        }
        passes.forEach(::executePass)
    }
}

data class Pipeline(
    val shader: ShaderId, val textureSlots: HashMap<Int, ImageId>, val viewport: Vec2i
)

data class DrawCommand(val pipeline: Pipeline, val mesh: MeshId, val uniforms: List<UniformSet>)

data class RenderPass(val framebuffer: FramebufferId, val draws: List<DrawCommand>)

data class FramebufferId(private val id: Int) {
    companion object {
        val default = FramebufferId(0)
    }

    internal class Generator {
        private var id = 0
        fun next() = FramebufferId(id++)
    }
}

data class ImageId(private val id: Int) {
    internal class Generator {
        private var id = 0
        fun next() = ImageId(id++)
    }
}

data class ShaderId(private val id: Int) {
    internal class Generator {
        private var id = 0
        fun next() = ShaderId(id++)
    }
}

data class MeshId(private val id: Int) {
    internal class Generator {
        private var id = 0
        fun next() = MeshId(id++)
    }
}

data class UniformId(private val id: Int) {
    internal class Generator {
        private var id = 0
        fun next() = UniformId(id++)
    }
}
