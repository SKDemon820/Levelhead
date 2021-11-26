package club.sk1er.mods.levelhead.render

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.Levelhead.displayManager
import club.sk1er.mods.levelhead.display.LevelheadTag
import club.sk1er.mods.levelhead.gui.LevelheadMainGUI
import gg.essential.api.EssentialAPI
import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.UMinecraft
import gg.essential.universal.UMinecraft.getFontRenderer
import gg.essential.universal.UMinecraft.getMinecraft
import gg.essential.universal.wrappers.UPlayer
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

object AboveHeadRender {

    @SubscribeEvent
    fun render(event: RenderLivingEvent.Specials.Post<EntityLivingBase>) {
        if (listOf(
                !displayManager.config.enabled,
                !(EssentialAPI.getMinecraftUtil().isHypixel() || getMinecraft().currentScreen is LevelheadMainGUI),
                getMinecraft().gameSettings.hideGUI
        ).any { it }) return

        if (event.entity !is EntityPlayer) return
        val player = event.entity as EntityPlayer

        displayManager.aboveHead.forEachIndexed { index, display ->
            if (index > Levelhead.LevelheadPurchaseStates.aboveHead
                || !display.config.enabled
                || (player.isSelf && !display.config.showSelf)) return@forEachIndexed
            val tag = display.cache[player.uniqueID]
            if (display.loadOrRender(player) && tag != null) {
                // increase offset if there's something in the above name slot for scoreboards
                var offset = 0.3
                if (player.worldScoreboard.getObjectiveInDisplaySlot(2) != null && player.getDistanceSqToEntity(UMinecraft.getPlayer()!!) < 100) {
                    offset *= 2
                }
                if (player.isSelf) offset = 0.0
                offset += displayManager.config.offset
                renderName(tag, player, event.x, event.y + offset + index * 0.3, event.z)
            }
        }
    }

    private val EntityPlayer.isSelf: Boolean
        get() = UPlayer.getPlayer()?.let { it.uniqueID == this.uniqueID } == true

    private fun renderName(tag: LevelheadTag, entityIn: EntityPlayer, x: Double, y: Double, z: Double) {
        val fontrenderer = getFontRenderer()
        val textScale = 0.016666668f * 1.6f * displayManager.config.fontSize
        GlStateManager.pushMatrix()
        val mc = getMinecraft()
        val xMultiplier = if (
            mc.gameSettings?.let { it.thirdPersonView == 2 } == true
        ) {
            -1
        } else {
            1
        }
        GlStateManager.translate(x.toFloat() + 0.0f, y.toFloat() + entityIn.height + 0.5f, z.toFloat())
        GL11.glNormal3f(0.0f, 1.0f, 0.0f)
        val renderManager = mc.renderManager
        GlStateManager.rotate(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(renderManager.playerViewX * xMultiplier, 1.0f, 0.0f, 0.0f)
        GlStateManager.scale(-textScale, -textScale, textScale)
        GlStateManager.disableLighting()
        GlStateManager.depthMask(false)
        GlStateManager.disableDepth()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        val stringWidth = fontrenderer.getStringWidth(tag.getString()) shr 1
        GlStateManager.disableTexture2D()
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR)
        worldrenderer.pos((-stringWidth - 1).toDouble(), -1.0, 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex()
        worldrenderer.pos((-stringWidth - 1).toDouble(), 8.0, 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex()
        worldrenderer.pos((stringWidth + 1).toDouble(), 8.0, 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex()
        worldrenderer.pos((stringWidth + 1).toDouble(), -1.0, 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex()
        tessellator.draw()
        GlStateManager.enableTexture2D()
        renderString(fontrenderer, tag)
        GlStateManager.enableLighting()
        GlStateManager.disableBlend()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.popMatrix()
    }

    private fun renderString(renderer: FontRenderer, tag: LevelheadTag) {
        var x = -renderer.getStringWidth(tag.getString()) shr 1
        //Render header
        render(renderer, tag.header, x)
        x += renderer.getStringWidth(tag.header.value)
        //render footer
        render(renderer, tag.footer, x)
    }

    private fun render(renderer: FontRenderer, component: LevelheadTag.LevelheadComponent, x: Int) {
        GlStateManager.disableDepth()
        GlStateManager.depthMask(true)
        GlStateManager.disableDepth()
        GlStateManager.depthMask(false)
        if (component.chroma) {
            renderer.drawString(component.value, x, 0, Levelhead.DarkChromaColor)
        } else {
            renderer.drawString(component.value, x, 0, component.color.withAlpha(0.2f).rgb)
        }
        GlStateManager.enableDepth()
        GlStateManager.depthMask(true)
        GlStateManager.color(1.0f, 1.0f, 1.0f)
        if (component.chroma) {
            renderer.drawString(component.value, x, 0, Levelhead.ChromaColor)
        } else {
            GlStateManager.color(
                component.color.red / 255f,
                component.color.green / 255f,
                component.color.blue / 255f,
                .5f
            )
            renderer.drawString(component.value, x, 0, component.color.rgb)
        }
    }
}