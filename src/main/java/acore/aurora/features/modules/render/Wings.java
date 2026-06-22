package acore.aurora.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;
import acore.aurora.utility.render.Render2DEngine;
import acore.aurora.utility.render.ThemeManager;

import java.awt.*;

public class Wings extends Module {
    public Wings() {
        super("Wings", Category.RENDER);
    }

    private final Setting<Float> size = new Setting<>("Size", 1.0f, 0.5f, 2.5f);
    private final Setting<Float> spread = new Setting<>("Spread", 1.0f, 0.5f, 2.0f);
    private final Setting<Float> offsetY = new Setting<>("OffsetY", 0.3f, -0.5f, 1.0f);
    private final Setting<Float> offsetZ = new Setting<>("OffsetZ", -0.15f, -0.5f, 0.5f);
    private final Setting<Boolean> animated = new Setting<>("Animated", true);
    private final Setting<Float> flapSpeed = new Setting<>("FlapSpeed", 1.2f, 0.2f, 3.0f);
    private final Setting<Boolean> glow = new Setting<>("Glow", true);
    private final Setting<Float> alpha = new Setting<>("Alpha", 0.85f, 0.1f, 1.0f);
    private final Setting<Boolean> hideSelf = new Setting<>("HideSelf", false);
    private final Setting<Boolean> showOthers = new Setting<>("ShowOthers", true);

    private float flapAngle = 0f;

    @Override
    public void onRender3D(MatrixStack stack) {
        if (fullNullCheck()) return;

        for (PlayerEntity entity : mc.world.getPlayers()) {
            boolean isSelf = entity == mc.player;

            if (isSelf && hideSelf.getValue()) continue;
            if (!isSelf && !showOthers.getValue()) continue;

            double camX = mc.getEntityRenderDispatcher().camera.getPos().getX();
            double camY = mc.getEntityRenderDispatcher().camera.getPos().getY();
            double camZ = mc.getEntityRenderDispatcher().camera.getPos().getZ();

            float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
            double x = entity.prevX + (entity.getX() - entity.prevX) * tickDelta - camX;
            double y = entity.prevY + (entity.getY() - entity.prevY) * tickDelta - camY;
            double z = entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta - camZ;

            float bodyYaw = entity.prevBodyYaw + (entity.bodyYaw - entity.prevBodyYaw) * tickDelta;
            float wingY = (float) y + entity.getHeight() * 0.55f + offsetY.getValue();
            float wingZ = (float) z + offsetZ.getValue();

            float flap = animated.getValue() ? (float) Math.sin(flapAngle) * 0.4f : 0f;

            int themeColor1 = ThemeManager.INSTANCE.getFirstColor();
            int themeColor2 = ThemeManager.INSTANCE.getSecondColor();
            int gradColor = ThemeManager.gradient(5, 0, themeColor1, themeColor2);

            Color col = new Color(
                (gradColor >> 16) & 0xFF,
                (gradColor >> 8) & 0xFF,
                gradColor & 0xFF,
                Math.max(0, Math.min(255, (int)(alpha.getValue() * 255)))
            );

            if (glow.getValue() && isSelf) {
                renderGlow(stack, (float) x, wingY, wingZ, bodyYaw, col);
            }

            renderWing(stack, (float) x, wingY, wingZ, bodyYaw, flap, false, col);
            renderWing(stack, (float) x, wingY, wingZ, bodyYaw, flap, true, col);
        }

        if (animated.getValue()) {
            flapAngle += 0.05f * flapSpeed.getValue();
        }
    }

    private void renderGlow(MatrixStack stack, float x, float y, float z, float yaw, Color col) {
        float s = size.getValue();
        float sp = spread.getValue();
        float glowR = s * 2.2f * sp;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        stack.push();
        stack.translate(x, y, z);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        Matrix4f mat = stack.peek().getPositionMatrix();

        int cr = col.getRed();
        int cg = col.getGreen();
        int cb = col.getBlue();

        int steps = 20;
        float[] perimX = new float[steps + 1];
        float[] perimY = new float[steps + 1];
        for (int i = 0; i <= steps; i++) {
            float angle = (float) (i * Math.PI * 2 / steps);
            perimX[i] = (float) Math.cos(angle) * glowR;
            perimY[i] = (float) Math.sin(angle) * glowR * 0.4f;
        }
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < steps; i++) {
            buf.vertex(mat, 0f, 0f, 0f).color(new Color(cr, cg, cb, 40).getRGB());
            buf.vertex(mat, perimX[i], perimY[i], 0f).color(new Color(cr, cg, cb, 0).getRGB());
            buf.vertex(mat, perimX[i + 1], perimY[i + 1], 0f).color(new Color(cr, cg, cb, 0).getRGB());
        }

        Render2DEngine.endBuilding(buf);
        stack.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void renderWing(MatrixStack stack, float x, float y, float z, float yaw, float flap, boolean right, Color col) {
        float s = size.getValue();
        float sp = spread.getValue();
        float side = right ? 1f : -1f;

        float w = s * 1.6f * sp;
        float h = s * 1.4f;

        // Wing points - xòe ngang ra như thật
        float[][] pts = {
            {0f, 0f},
            {w * 0.2f, -h * 0.05f + flap * h * 0.15f},
            {w * 0.45f, -h * 0.15f + flap * h * 0.2f},
            {w * 0.70f, -h * 0.2f + flap * h * 0.25f},
            {w * 0.90f, -h * 0.15f + flap * h * 0.15f},
            {w * 1.00f, -h * 0.05f + flap * h * 0.1f},
            {w * 0.95f, h * 0.05f},
            {w * 0.70f, h * 0.1f - flap * h * 0.05f},
            {w * 0.40f, h * 0.08f - flap * h * 0.05f},
            {w * 0.15f, h * 0.03f},
            {0f, 0f}
        };

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        stack.push();
        stack.translate(x, y, z);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        stack.scale(side, 1f, 1f);
        Matrix4f mat = stack.peek().getPositionMatrix();

        int cr = col.getRed();
        int cg = col.getGreen();
        int cb = col.getBlue();
        int ca = col.getAlpha();

        float cx2 = w * 0.5f;
        float cy2 = 0f;
        int centerColor = new Color(cr, cg, cb, ca).getRGB();

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < pts.length - 1; i++) {
            float tx0 = pts[i][0], ty0 = pts[i][1];
            float tx1 = pts[i + 1][0], ty1 = pts[i + 1][1];
            float f0 = (float) Math.sqrt(tx0 * tx0 + ty0 * ty0) / (float) Math.sqrt(w * w + h * h);
            float f1 = (float) Math.sqrt(tx1 * tx1 + ty1 * ty1) / (float) Math.sqrt(w * w + h * h);
            int a0 = (int) (ca * (0.5f + 0.5f * (1f - f0)));
            int a1 = (int) (ca * (0.5f + 0.5f * (1f - f1)));
            buf.vertex(mat, cx2, cy2, 0f).color(centerColor);
            buf.vertex(mat, tx0, ty0, 0f).color(new Color(cr, cg, cb, a0).getRGB());
            buf.vertex(mat, tx1, ty1, 0f).color(new Color(cr, cg, cb, a1).getRGB());
        }

        Render2DEngine.endBuilding(buf);
        stack.pop();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }
    }
