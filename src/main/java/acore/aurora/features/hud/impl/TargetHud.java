package acore.aurora.features.hud.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.util.Colors;
import org.lwjgl.opengl.GL40C;
import acore.aurora.core.manager.client.ModuleManager;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.gui.hud.HudEditorGui;
import acore.aurora.features.hud.HudElement;
import acore.aurora.features.modules.render.HudEditor;
import acore.aurora.features.modules.combat.Aura;
import acore.aurora.features.modules.misc.NameProtect;
import acore.aurora.setting.Setting;
import acore.aurora.utility.Timer;
import acore.aurora.utility.math.MathUtility;
import acore.aurora.utility.render.Render2DEngine;
import acore.aurora.utility.render.Render3DEngine;
import acore.aurora.utility.render.animation.EaseOutBack;
import acore.aurora.utility.render.animation.EaseOutCirc;

import java.awt.*;
import java.util.List;

public class TargetHud extends HudElement {
    private final Setting<Integer> animX = new Setting<>("AnimationX", 0, -2000, 2000);
    private final Setting<Integer> animY = new Setting<>("AnimationY", 0, -2000, 2000);
    private final Setting<HPmodeEn> hpMode = new Setting<>("HP Mode", HPmodeEn.HP);
    private final Setting<Boolean> funTimeHP = new Setting<>("FunTimeHP", false);
    private final Setting<Boolean> mini = new Setting<>("Mini", false);
    private final Setting<Boolean> absorp = new Setting<>("Absorption", true);

    public EaseOutBack animation = new EaseOutBack();
    public static EaseOutCirc healthAnimation = new EaseOutCirc();

    private boolean direction = false;
    private LivingEntity target;

    private final Timer timer = new Timer();

    public TargetHud() {
        super("TargetHud", 150, 50);
    }

    @Override
    public void onUpdate() {
        animation.update(direction);
        healthAnimation.update();
    }

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        getTarget();
        if (target == null) return;

        float health = Math.min(target.getMaxHealth(), getHealth());

        context.getMatrices().push();

        if (!HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            if (mini.getValue())
                sizeAnimation(context.getMatrices(), getPosX() + 45 + animX.getValue(), getPosY() + 15 + animY.getValue(), animation.getAnimationd());
            else
                sizeAnimation(context.getMatrices(), getPosX() + 75 + animX.getValue(), getPosY() + 25 + animY.getValue(), animation.getAnimationd());
        }

        if (animation.getAnimationd() > 0) {
            float animationFactor = (float) MathUtility.clamp(animation.getAnimationd(), 0, 1f);
            if (mini.getValue())
                renderMiniNurik(context, health, animationFactor);
            else
                renderNurik(context, health, animationFactor);
        }
        context.getMatrices().pop();
    }

    private void getTarget() {
        if (Aura.target != null) {
            if (Aura.target instanceof LivingEntity) {
                target = (LivingEntity) Aura.target;
                direction = true;
            } else {
                target = null;
                direction = false;
            }
        } else if (mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof HudEditorGui) {
            target = mc.player;
            direction = true;
        } else {
            direction = false;
            if (animation.getAnimationd() < 0.02)
                target = null;
        }
    }

    // Chấm trạng thái máu ở góc trên bên phải: xanh khi máu mình cao hơn đối thủ, đỏ khi thấp hơn
    private void drawHealthDot(DrawContext context, float dotCenterX, float dotCenterY, float animationFactor) {
        float self = mc.player != null ? mc.player.getHealth() + mc.player.getAbsorptionAmount() : 0f;
        boolean higher = mc.player == null || target == mc.player || self >= getHealth();
        Color dot = higher ? new Color(76, 255, 122) : new Color(255, 76, 76); // xanh / đỏ
        Color glow = Render2DEngine.applyOpacity(dot, animationFactor * 0.7f);
        Color fill = Render2DEngine.applyOpacity(dot, animationFactor);

        float r = 3.2f; // nét to vừa
        // Glow nền
        Render2DEngine.drawBlurredShadow(context.getMatrices(), dotCenterX - r - 1, dotCenterY - r - 1, (r + 1) * 2, (r + 1) * 2, 6, glow);
        // Chấm tròn đặc (round với bo góc = bán kính -> hình tròn)
        Render2DEngine.drawRound(context.getMatrices(), dotCenterX - r, dotCenterY - r, r * 2, r * 2, r, fill);
    }

    private void renderNurik(DrawContext context, float health, float animationFactor) {
        float hurtPercent = (Render2DEngine.interpolateFloat(MathUtility.clamp(target.hurtTime == 0 ? 0 : target.hurtTime + 1, 0, 10), target.hurtTime, Render3DEngine.getTickDelta())) / 8f;
        healthAnimation.setValue(health);
        health = (float) healthAnimation.getAnimationD();

        if (animation.getAnimationd() != 1 && !HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DEngine.drawGradientBlurredShadow1(context.getMatrices(), getPosX() + 4, getPosY() + 4, 131, 40, 14, HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));
            Render2DEngine.renderRoundedGradientRect(context.getMatrices(), HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90), getPosX(), getPosY(), 137, 47.5f, 9);
            Render2DEngine.drawRound(context.getMatrices(), getPosX() + 0.5f, getPosY() + 0.5f, 136f, 46, 9, Render2DEngine.injectAlpha(Color.BLACK, 220));
        } else
            Render2DEngine.drawHudBase2(context.getMatrices(), getPosX(), getPosY(), 137f, 47.5f, 9f, HudEditor.blurStrength.getValue(), HudEditor.blurOpacity.getValue(), animationFactor);

        setBounds(getPosX(), getPosY(), 137, 47.5f);

        // Бошка
        if (target instanceof PlayerEntity) {
            RenderSystem.setShaderTexture(0, ((AbstractClientPlayerEntity) target).getSkinTextures().texture());
        } else {
            RenderSystem.setShaderTexture(0, mc.getEntityRenderDispatcher().getRenderer(target).getTexture(target));
        }

        context.getMatrices().push();
        context.getMatrices().translate(getPosX() + 3.5f + 20, getPosY() + 3.5f + 20, 0);
        context.getMatrices().scale(1 - hurtPercent / 15f, 1 - hurtPercent / 15f, 1f);
        context.getMatrices().translate(-(getPosX() + 3.5f + 20), -(getPosY() + 3.5f + 20), 0);
        RenderSystem.enableBlend();
        RenderSystem.colorMask(false, false, false, true);
        RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
        RenderSystem.clear(GL40C.GL_COLOR_BUFFER_BIT, false);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Render2DEngine.renderRoundedQuadInternal(context.getMatrices().peek().getPositionMatrix(), animationFactor, animationFactor, animationFactor, animationFactor, getPosX() + 3.5f, getPosY() + 3.5f, getPosX() + 3.5f + 40, getPosY() + 3.5f + 40, 7, 10);
        RenderSystem.blendFunc(GL40C.GL_DST_ALPHA, GL40C.GL_ONE_MINUS_DST_ALPHA);
        RenderSystem.setShaderColor(animationFactor, animationFactor - hurtPercent / 2, animationFactor - hurtPercent / 2, (float) MathUtility.clamp(animation.getAnimationd(), 0, 1f));
        Render2DEngine.renderTexture(context.getMatrices(), getPosX() + 3.5f, getPosY() + 3.5f, 40, 40, 8, 8, 8, 8, 64, 64);
        Render2DEngine.renderTexture(context.getMatrices(), getPosX() + 3.5f, getPosY() + 3.5f, 40, 40, 40, 8, 8, 8, 64, 64);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        context.getMatrices().pop();

        // Баллон
        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DEngine.drawRect(context.getMatrices(), getPosX() + 48, getPosY() + 32, 85f, 11f, 4f, (float) (0.15f * animation.getAnimationd()));
            Render2DEngine.drawRect(context.getMatrices(), getPosX() + 48, getPosY() + 32, MathUtility.clamp((85 * (health / target.getMaxHealth())), 8, 85), 11f, 4f, (float) (animation.getAnimationd()));
        } else {
            Render2DEngine.drawGradientRound(context.getMatrices(), getPosX() + 48, getPosY() + 32, 85, 11, 4f, HudEditor.getColor(0).darker().darker(), HudEditor.getColor(0).darker().darker().darker().darker(), HudEditor.getColor(0).darker().darker().darker().darker(), HudEditor.getColor(0).darker().darker().darker().darker());
            Render2DEngine.renderRoundedGradientRect(context.getMatrices(), HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(0), HudEditor.getColor(270), getPosX() + 48, getPosY() + 32, (int) MathUtility.clamp((85 * (health / target.getMaxHealth())), 8, 85), 11, 4f);
        }

        FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), hpMode.getValue() == HPmodeEn.HP ? String.valueOf(Math.round(10.0 * getHealth()) / 10.0) : (((Math.round(10.0 * getHealth()) / 10.0) / 20f) * 100 + "%"), getPosX() + 92f, getPosY() + 35f,
                Render2DEngine.applyOpacity(Colors.WHITE, animationFactor));

        //Имя
        FontRenderers.sf_bold.drawString(context.getMatrices(), ModuleManager.nameProtect.isEnabled() && target == mc.player ? NameProtect.getCustomName() : target.getName().getString(), getPosX() + 48, getPosY() + 7,
                Render2DEngine.applyOpacity(Colors.WHITE, animationFactor));

        if (target instanceof PlayerEntity) {
            RenderSystem.setShaderColor(1f, 1f, 1f, (float) MathUtility.clamp(animation.getAnimationd(), 0, 1f));

            //Броня
            List<ItemStack> armor = ((PlayerEntity) target).getInventory().armor;
            ItemStack[] items = new ItemStack[]{target.getMainHandStack(), armor.get(3), armor.get(2), armor.get(1), armor.get(0), target.getOffHandStack()};

            float xItemOffset = getPosX() + 48;
            for (ItemStack itemStack : items) {
                context.getMatrices().push();
                context.getMatrices().translate(xItemOffset, getPosY() + 15, 0);
                context.getMatrices().scale(0.75f, 0.75f, 0.75f);
                context.drawItem(itemStack, 0, 0);
                context.drawItemInSlot(mc.textRenderer, itemStack, 0, 0);
                context.getMatrices().pop();
                xItemOffset += 12;
            }
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        // Chấm trạng thái máu ở góc trên bên phải
        drawHealthDot(context, getPosX() + 137 - 7, getPosY() + 7, animationFactor);
    }

    private void renderMiniNurik(DrawContext context, float health, float animationFactor) {
        float hurtPercent = (Render2DEngine.interpolateFloat(MathUtility.clamp(target.hurtTime == 0 ? 0 : target.hurtTime + 1, 0, 10), target.hurtTime, Render3DEngine.getTickDelta())) / 8f;
        healthAnimation.setValue(health);
        health = (float) healthAnimation.getAnimationD();

        // Основа
        if (animation.getAnimationd() != 1 && !HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DEngine.drawGradientBlurredShadow1(context.getMatrices(), getPosX() + 2, getPosY() + 2, 91, 31, 12, HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));
            Render2DEngine.renderRoundedGradientRect(context.getMatrices(), HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90), getPosX(), getPosY(), 95, 35, 7);
            Render2DEngine.drawRound(context.getMatrices(), getPosX() + 0.5f, getPosY() + 0.5f, 94, 34, 7, Render2DEngine.injectAlpha(Color.BLACK, 220));
        } else
            Render2DEngine.drawHudBase2(context.getMatrices(), getPosX(), getPosY(), 95, 35.5f, 8, HudEditor.blurStrength.getValue(), HudEditor.blurOpacity.getValue(), animationFactor);

        setBounds(getPosX(), getPosY(), 95, 35.5f);

        // Бошка
        if (target instanceof PlayerEntity) {
            RenderSystem.setShaderTexture(0, ((AbstractClientPlayerEntity) target).getSkinTextures().texture());
        } else {
            RenderSystem.setShaderTexture(0, mc.getEntityRenderDispatcher().getRenderer(target).getTexture(target));
        }

        context.getMatrices().push();
        context.getMatrices().translate(getPosX() + 2.5 + 15, getPosY() + 2.5 + 15, 0);
        context.getMatrices().scale(1 - hurtPercent / 20f, 1 - hurtPercent / 20f, 1f);
        context.getMatrices().translate(-(getPosX() + 2.5 + 15), -(getPosY() + 2.5 + 15), 0);
        RenderSystem.enableBlend();
        RenderSystem.colorMask(false, false, false, true);
        RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
        RenderSystem.clear(GL40C.GL_COLOR_BUFFER_BIT, false);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Render2DEngine.renderRoundedQuadInternal(context.getMatrices().peek().getPositionMatrix(), animationFactor, animationFactor, animationFactor, animationFactor, getPosX() + 2.5, getPosY() + 2.5, getPosX() + 2.5 + 30, getPosY() + 2.5 + 30, 5, 10);
        RenderSystem.blendFunc(GL40C.GL_DST_ALPHA, GL40C.GL_ONE_MINUS_DST_ALPHA);
        RenderSystem.setShaderColor(animationFactor, animationFactor - hurtPercent / 2, animationFactor - hurtPercent / 2, animationFactor);
        Render2DEngine.renderTexture(context.getMatrices(), getPosX() + 2.5, getPosY() + 2.5, 30, 30, 8, 8, 8, 8, 64, 64);
        Render2DEngine.renderTexture(context.getMatrices(), getPosX() + 2.5, getPosY() + 2.5, 30, 30, 40, 8, 8, 8, 64, 64);
        RenderSystem.defaultBlendFunc();
        context.getMatrices().pop();

        // Баллон
        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DEngine.drawRect(context.getMatrices(), getPosX() + 38, getPosY() + 25, 52f, 7f, 2f, (float) (0.15f * animation.getAnimationd()));
            Render2DEngine.drawRect(context.getMatrices(), getPosX() + 38, getPosY() + 25, MathUtility.clamp((52f * (health / target.getMaxHealth())), 8, 52), 7f, 2f, (float) (animation.getAnimationd()));
        } else {
            Render2DEngine.drawGradientRound(context.getMatrices(), getPosX() + 38, getPosY() + 25, 52, 7, 2f, HudEditor.getColor(0).darker().darker(), HudEditor.getColor(0).darker().darker().darker().darker(), HudEditor.getColor(0).darker().darker().darker().darker(), HudEditor.getColor(0).darker().darker().darker().darker());
            Render2DEngine.renderRoundedGradientRect(context.getMatrices(), HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(0), HudEditor.getColor(270), getPosX() + 38, getPosY() + 25, (int) MathUtility.clamp((52 * (health / target.getMaxHealth())), 8, 52), 7, 2f);
        }

        FontRenderers.sf_bold_mini.drawCenteredString(context.getMatrices(), hpMode.getValue() == HPmodeEn.HP ? String.valueOf(Math.round(10.0 * getHealth()) / 10.0) : (((Math.round(10.0 * getHealth()) / 10.0) / 20f) * 100 + "%"), getPosX() + 65, getPosY() + 27f, Render2DEngine.applyOpacity(Colors.WHITE, animationFactor));

        //Имя
        FontRenderers.sf_bold_mini.drawString(context.getMatrices(), ModuleManager.nameProtect.isEnabled() && target == mc.player ? NameProtect.getCustomName() : target.getName().getString(), getPosX() + 38, getPosY() + 5, Render2DEngine.applyOpacity(Colors.WHITE, animationFactor));

        if (target instanceof PlayerEntity) {
            //Броня
            RenderSystem.setShaderColor(1f, 1f, 1f, (float) MathUtility.clamp(animation.getAnimationd(), 0, 1f));
            List<ItemStack> armor = ((PlayerEntity) target).getInventory().armor;
            ItemStack[] items = new ItemStack[]{target.getMainHandStack(), armor.get(3), armor.get(2), armor.get(1), armor.get(0), target.getOffHandStack()};

            float xItemOffset = getPosX() + 38;
            for (ItemStack itemStack : items) {
                context.getMatrices().push();
                context.getMatrices().translate(xItemOffset, getPosY() + 13, 0);
                context.getMatrices().scale(0.5f, 0.5f, 0.5f);
                context.drawItem(itemStack, 0, 0);
                context.drawItemInSlot(mc.textRenderer, itemStack, 0, 0);
                context.getMatrices().pop();
                xItemOffset += 9;
            }
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        // Chấm trạng thái máu ở góc trên bên phải
        drawHealthDot(context, getPosX() + 95 - 6, getPosY() + 6, animationFactor);
    }

    public float getHealth() {
        // Первый в комьюнити хп резольвер. Правда, еж?
        if (target instanceof PlayerEntity ent && (mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null && mc.getNetworkHandler().getServerInfo().address.contains("funtime") || funTimeHP.getValue())) {
            ScoreboardObjective scoreBoard = null;
            String resolvedHp = "";
            if ((ent.getScoreboard()).getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
                scoreBoard = (ent.getScoreboard()).getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
                if (scoreBoard != null) {
                    ReadableScoreboardScore readableScoreboardScore = ent.getScoreboard().getScore(ent, scoreBoard);
                    MutableText text2 = ReadableScoreboardScore.getFormattedScore(readableScoreboardScore, scoreBoard.getNumberFormatOr(StyledNumberFormat.EMPTY));
                    resolvedHp = text2.getString();
                }
            }
            float numValue = 0;
            try {
                numValue = Float.parseFloat(resolvedHp);
            } catch (NumberFormatException ignored) {
            }
            return (absorp.getValue()) ? numValue + target.getAbsorptionAmount() : numValue;
        } else return (absorp.getValue()) ? target.getHealth() + target.getAbsorptionAmount() : target.getHealth();
    }

    public static void sizeAnimation(MatrixStack matrixStack, double width, double height, double animation) {
        matrixStack.translate(width, height, 0);
        matrixStack.scale((float) animation, (float) animation, 1);
        matrixStack.translate(-width, -height, 0);
    }

    public enum HPmodeEn {
        HP, Percentage
    }
                                        }
