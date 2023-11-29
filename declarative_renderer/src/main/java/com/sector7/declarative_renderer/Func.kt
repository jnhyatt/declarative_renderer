package com.sector7.declarative_renderer

import com.sector7.math.Mat4f
import com.sector7.math.Vec3f
import kotlin.math.max

class FuncVars {
    val floatVars = HashMap<Int, Float>()

    fun FloatFunc.eval(): Float = when (this) {
        is FloatValue -> value
        is FloatVar -> floatVars[index]!!
        is FloatAdd -> lhs.eval() + rhs.eval()
        is FloatSubtract -> lhs.eval() - rhs.eval()
        is FloatDivide -> lhs.eval() / rhs.eval()
        is FloatMax -> max(a.eval(), b.eval())
    }

    fun Vec3fFunc.eval(): Vec3f = when (this) {
        is Vec3fValue -> value
        is Vec3fScale -> vec.eval() * scale.eval()
    }

    fun Mat4fFunc.eval() = when (this) {
        is Mat4fValue -> value
        is Mat4fTranslation -> Mat4f.translation(amount.eval())
        is Mat4fPerspective -> Mat4f.perspective(
            fovY.eval(), aspectRatio.eval(), clipNear.eval(), clipFar.eval()
        )
    }
}

sealed class FloatFunc {
    override fun toString() = when (this) {
        is FloatValue -> "$value"
        is FloatVar -> "var$index"
        is FloatAdd -> "($lhs + $rhs)"
        is FloatSubtract -> "($lhs - $rhs)"
        is FloatDivide -> "($lhs / $rhs)"
        is FloatMax -> "max($a, $b)"
    }
}

class FloatValue(val value: Float) : FloatFunc()
class FloatVar(val index: Int) : FloatFunc()
class FloatAdd(val lhs: FloatFunc, val rhs: FloatFunc) : FloatFunc()
class FloatSubtract(val lhs: FloatFunc, val rhs: FloatFunc) : FloatFunc()
class FloatDivide(val lhs: FloatFunc, val rhs: FloatFunc) : FloatFunc()
class FloatMax(val a: FloatFunc, val b: FloatFunc) : FloatFunc()

operator fun FloatFunc.plus(rhs: FloatFunc) = FloatAdd(this, rhs)
operator fun FloatFunc.minus(rhs: FloatFunc) = FloatSubtract(this, rhs)
operator fun FloatFunc.div(rhs: FloatFunc) = FloatDivide(this, rhs)

fun max(a: FloatFunc, b: FloatFunc) = FloatMax(a, b)

sealed class Vec3fFunc {
    override fun toString() = when (this) {
        is Vec3fValue -> "$value"
        is Vec3fScale -> "($scale * $vec)"
    }
}

class Vec3fValue(val value: Vec3f) : Vec3fFunc()
class Vec3fScale(val vec: Vec3fFunc, val scale: FloatFunc) : Vec3fFunc()

operator fun Vec3fFunc.times(rhs: FloatFunc) = Vec3fScale(this, rhs)

sealed class Mat4fFunc {
    override fun toString() = when (this) {
        is Mat4fValue -> "$value"
        is Mat4fTranslation -> "translation($amount)"
        is Mat4fPerspective -> "perspective(fovY: $fovY, aspectRatio: $aspectRatio, clipNear: $clipNear, clipFar: $clipFar)"
    }
}

class Mat4fValue(val value: Mat4f) : Mat4fFunc()
class Mat4fTranslation(val amount: Vec3fFunc) : Mat4fFunc()
class Mat4fPerspective(
    val fovY: FloatFunc, val aspectRatio: FloatFunc, val clipNear: FloatFunc, val clipFar: FloatFunc
) : Mat4fFunc()

val Float.func get() = FloatValue(this)
val Vec3f.func get() = Vec3fValue(this)
val Mat4f.func get() = Mat4fValue(this)
