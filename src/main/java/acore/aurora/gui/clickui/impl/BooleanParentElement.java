package acore.aurora.gui.clickui.impl;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import acore.aurora.core.Managers;
import acore.aurora.features.modules.render.HudEditor;
import acore.aurora.gui.clickui.AbstractElement;
import acore.aurora.gui.clickui.ClickGUI;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.BooleanSettingGroup;
import acore.aurora.utility.render.Render2DEngine;
import acore.aurora.utility.render.TextureStorage;
import java.awt.*;
import static acore.aurora.core.manager.IManager.mc;
import static acore.aurora.utility.render.animation.AnimationUtility.fast;
public class BooleanParentElement extends AbstractElement {
    private final Setting<BooleanSettingGroup> parentSetting;
    private float anim, arrowAnim;
    private static final Color TRACK_OFF = new Color(50, 50, 62, 220);
    private static final Color KNOB      = new Color(248, 248, 250, 255);
    private static final Color NAME_COL  = new Color(200, 200, 212, 255);
    public BooleanParentElement(Setting setting) {
        super(setting);
        this.parentSetting = setting;
    }
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        anim      = fast(anim,      parentSetting.getValue().isEnabled()  ? 1f : 0f, 20f);
        arrowAnim = fast(arrowAnim, parentSetting.getValue().isExtended() ? 0f : 1f, 15f);
        MatrixStack ms = ctx.getMatrices();
        float tx = x + width - 11;
        float ty = y + 7.5f;
        ms.push();
        ms.translate(tx, ty, 0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-180f * arrowAnim));
        ms.translate(-tx, -ty, 0);
        ms.translate(x + width - 14, y + 4.5f, 0);
        ctx.drawTexture(TextureStorage.guiArrow, 0, 0, 0, 0, 6, 6, 6, 6);
        ms.translate(-(x + width - 14), -(y + 4.5f), 0);
        ms.pop();
        FontRenderers.sf_medium_mini.drawString(ms, setting.getName(),
                x + 6, y + height / 2f - 1f, NAME_COL.getRGB());
        float trackW = 20f;
        float trackH = 9f;
        float trackX = x + width - trackW - 20;
        float trackY = y + height / 2f - trackH / 2f;
        Color raw = HudEditor.getColor(0);
        Color accent = (raw.getAlpha() < 10 || (raw.getRed() < 5 && raw.getGreen() < 5 && raw.getBlue() < 5))
                ? new Color(200, 80, 80, 255) : raw;
        int cr = (int)(TRACK_OFF.getRed()   + (accent.getRed()   - TRACK_OFF.getRed())   * anim);
        int cg = (int)(TRACK_OFF.getGreen() + (accent.getGreen() - TRACK_OFF.getGreen()) * anim);
        int cb = (int)(TRACK_OFF.getBlue()  + (accent.getBlue()  - TRACK_OFF.getBlue())  * anim);
        Render2DEngine.drawRound(ms, trackX, trackY, trackW, trackH, 5f, new Color(cr, cg, cb, 220));
        float ks = trackH - 3f;
        float kt = trackW - ks - 3f;
        Render2DEngine.drawRound(ms, trackX + 1.5f + kt * anim, trackY + 1.5f, ks, ks, 4f, KNOB);
        if (Render2DEngine.isHovered(mx, my, trackX, trackY, trackW, trackH)) {
            if (GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_WAYLAND)
                GLFW.glfwSetCursor(mc.getWindow().getHandle(),
                        GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR));
            ClickGUI.anyHovered = true;
        }
    }
    @Override
    public void mouseClicked(int mx, int my, int button) {
        if (hovered) {
            float trackW = 20f;
            float trackH = 9f;
            float trackX = x + width - trackW - 20;
            float trackY = y + height / 2f - trackH / 2f;
            if (Render2DEngine.isHovered(mx, my, trackX, trackY, trackW, trackH)) {
                parentSetting.getValue().setEnabled(!parentSetting.getValue().isEnabled());
                Managers.SOUND.playBoolean();
            } else {
                parentSetting.getValue().setExtended(!parentSetting.getValue().isExtended());
                if (parentSetting.getValue().isExtended()) Managers.SOUND.playSwipeIn();
                else Managers.SOUND.playSwipeOut();
            }
        }
        super.mouseClicked(mx, my, button);
    }
    public Setting<BooleanSettingGroup> getParentSetting() { return parentSetting; }
                        }
                    
