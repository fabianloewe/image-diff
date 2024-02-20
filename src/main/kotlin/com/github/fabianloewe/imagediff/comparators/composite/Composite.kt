package com.github.fabianloewe.imagediff.comparators.composite

import com.github.fabianloewe.imagediff.Image
import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.composite.*
import thirdparty.romainguy.BlendComposite
import thirdparty.romainguy.BlendingMode
import java.awt.Graphics2D

class ExclusionComposite(private val alpha: Double) : Composite {
    override fun apply(src: AwtImage, overlay: AwtImage) {
        val g2 = src.awt().graphics as Graphics2D
        g2.composite = BlendComposite.getInstance(BlendingMode.EXCLUSION, alpha.toFloat())
        g2.drawImage(overlay.awt(), 0, 0, null)
        g2.dispose()
    }
}

class DarkenComposite(private val alpha: Double) : Composite {
    override fun apply(src: AwtImage, overlay: AwtImage) {
        val g2 = src.awt().graphics as Graphics2D
        g2.composite = BlendComposite.getInstance(BlendingMode.DARKEN, alpha.toFloat())
        g2.drawImage(overlay.awt(), 0, 0, null)
        g2.dispose()
    }
}

fun CompositeType.toComposite(alpha: Double = 1.0): Composite = when (this) {
    CompositeType.DIFF -> DifferenceComposite(alpha)
    CompositeType.SUBTRACT -> SubtractComposite(alpha)
    CompositeType.OVERLAY -> OverlayComposite(alpha)
    CompositeType.EXCLUSION -> ExclusionComposite(alpha)
    CompositeType.LIGHTEN -> LightenComposite(alpha)
    CompositeType.DARKEN -> DarkenComposite(alpha)
}

operator fun CompositeType.invoke(image: Image, other: Image): Image {
    val compositor = toComposite()
    return image.copy(
        _data = image.data.composite(compositor, other.data)
    )
}