package acore.aurora.gui.clickui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import acore.aurora.core.Managers;
import acore.aurora.core.manager.client.ModuleManager;
import acore.aurora.features.modules.render.AcoreAuroraGui;
import acore.aurora.features.modules.render.ClickGui;
import acore.aurora.features.modules.Module;
import acore.aurora.gui.clickui.impl.*;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.PositionSetting;
import acore.aurora.utility.color.ColorUtil;
import acore.aurora.utility.render.Render2DEngine;
import acore.aurora.utility.render.ThemeManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import static acore.aurora.features.modules.Module.mc;

public class ClickGUI extends Screen {

    public static boolean anyHovered = false;
    public static boolean close      = false;
    public static String currentDescription = "";
    public acore.aurora.utility.render.animation.EaseOutBack imageAnimation =
            new acore.aurora.utility.render.animation.EaseOutBack();

    private boolean isClose = false;

    private static final int   PANEL_WIDTH       = 125;
    private static final int   PANEL_HEIGHT      = 280;
    private static final int   PANEL_MARGIN      = 8;
    private static final int   PANEL_RADIUS      = 12;
    private static final int   TITLE_MARGIN_TOP  = 5;
    private static final int   TITLE_HEIGHT      = 20;
    private static final int   FUNCTION_HEIGHT   = 20;
    private static final int   SCROLL_AREA_Y     = TITLE_MARGIN_TOP + TITLE_HEIGHT;
    private static final int   SCROLL_AREA_H     = PANEL_HEIGHT - SCROLL_AREA_Y - 5;
    private static final int   SEARCH_HEIGHT     = 14;
    private static final int   SEARCH_MARGIN_BOT = 8;
    private static final int   SEARCH_MAX_WIDTH  = 140;
    private static final int   THEME_HEIGHT      = 12;
    private static final int   THEME_MARGIN_BOT  = 28;
    private static final int   THEME_MAX_WIDTH   = 140;
    private static final int   VISIBLE_THEMES    = 11;
    private static final float SCROLL_SPEED      = 12f;
    private static final float SCROLL_LERP       = 20f;
    private static final float THEME_SCROLL_SPEED = 15f;
    private static final float THEME_SCROLL_LERP  = 15f;
    private static final float THEME_ANIM_SPEED   = 0.2f;
    private static final int   MODULE_BG_ALPHA   = 20;
    private static final int   MODULE_ROUNDING   = 6;

    private static final Color GUI_COLOR          = new Color(15, 15, 20, 225);
    private static final Color PANEL_BORDER_COLOR = new Color(0, 0, 0, 40);
    private static final Color PANEL_BG_COLOR     = new Color(22, 26, 35, 130);
    private static final Color MODULE_OFF_COLOR   = new Color(198, 198, 198);
    private static final Color MODULE_OFF_BG_COLOR = new Color(198, 198, 198, MODULE_BG_ALPHA);
    private static final Color SCROLLBAR_TRACK_COLOR = new Color(0, 0, 0, 50);
    private static final Color SCROLLBAR_THUMB_COLOR = new Color(255, 255, 255, 150);
    private static final Color SEARCH_HINT_COLOR  = new Color(255, 255, 255, 120);

    private static final Map<String, String> CAT_ICONS = new HashMap<>();
    static {
        CAT_ICONS.put("Combat",   "f");
        CAT_ICONS.put("Movement", "w");
        CAT_ICONS.put("Render",   "E");
        CAT_ICONS.put("Player",   "r");
        CAT_ICONS.put("Misc",     "v");
    }

    private static final List<Module.Category> CATS = Arrays.asList(
        Module.Category.COMBAT, Module.Category.MOVEMENT,
        Module.Category.RENDER, Module.Category.PLAYER, Module.Category.MISC);

    private static final Map<Module.Category, Float> scrollOffsets = new HashMap<>();
    private static final Map<Module.Category, Float> scrollTargets = new HashMap<>();
    private final Map<Module, Float> expandProgress = new HashMap<>();
    private final Map<Module, Float> arrowProgress  = new HashMap<>();

    private float openAnim = 0f;

    private static float themeScrollOffset = 0f;
    private static float themeScrollTarget = 0f;
    private float themeMenuAnim   = 0f;
    private float themeMenuTarget = 0f;
    private float themeAlphaAnim  = 0f;
    private float themeNameAnim   = 0f;
    private static boolean themeMenu = false;

    private static boolean colorPickerOpen = false;
    private float colorPickerAnim = 0f;
    private float picker1CursorX = 0.5f, picker1CursorY = 0.5f;
    private float picker2CursorX = 0.5f, picker2CursorY = 0.5f;
    private boolean draggingPicker1 = false, draggingPicker2 = false;
    private static int selectedColor1 = -1;
    private static int selectedColor2 = -1;
    private static BufferedImage colorImage = null;

    private final SearchState searchState = new SearchState();
    private boolean functionBinding = false;
    private Module bindingModule = null;

    private Setting<?> draggingSlider = null;
    private int draggingSliderX = 0;
    private int draggingSliderW = 0;

    private final ExosToggleSwitchRenderer boolRend   = new ExosToggleSwitchRenderer();
    private final ExosSliderRenderer       sliderRend = new ExosSliderRenderer();
    private final ExosModeRenderer         modeRend   = new ExosModeRenderer();

    public ClickGUI() {
        super(Text.literal("ClickGUI"));
        if (scrollOffsets.isEmpty()) {
            CATS.forEach(c -> { scrollOffsets.put(c, 0f); scrollTargets.put(c, 0f); });
        }
        loadColorImage();
    }

    private void loadColorImage() {
        if (colorImage != null) return;
        try {
            colorImage = ImageIO.read(Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(
                    "assets/acoreaurora/textures/gui/elements/pick.png")));
        } catch (Exception ignored) {}
    }

    private int getPixelColor(float u, float v) {
        if (colorImage == null) return Color.WHITE.getRGB();
        int x = MathHelper.clamp((int)(u * colorImage.getWidth()),  0, colorImage.getWidth()  - 1);
        int y = MathHelper.clamp((int)(v * colorImage.getHeight()), 0, colorImage.getHeight() - 1);
        return colorImage.getRGB(x, y);
    }

    public static ClickGUI getClickGui() { return new ClickGUI(); }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    @Override
    public void init() {
        super.init();
        isClose = false;
        openAnim = 0f;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (isClose && openAnim <= 0.01f) { mc.setScreen(null); return; }
        openAnim = isClose
            ? Math.max(0f, openAnim - delta * 5f)
            : Math.min(1f, openAnim + delta * 5f);
        if (openAnim <= 0.01f) return;

        CATS.forEach(c -> {
            float t = scrollTargets.get(c), o = scrollOffsets.get(c);
            scrollOffsets.put(c, lerp(o, t, SCROLL_LERP * delta));
        });
        themeScrollOffset = lerp(themeScrollOffset, themeScrollTarget, THEME_SCROLL_LERP * delta);
        themeMenuAnim    += (themeMenuTarget - themeMenuAnim)  * THEME_ANIM_SPEED;
        float tAlpha = themeMenuTarget > 0.01f ? 1f : 0f;
        themeAlphaAnim   += (tAlpha - themeAlphaAnim) * 0.15f;
        colorPickerAnim  += ((colorPickerOpen ? 1f : 0f) - colorPickerAnim) * 0.2f;

        int totalW = CATS.size() * (PANEL_WIDTH + PANEL_MARGIN) - PANEL_MARGIN;
        int startX = (width - totalW) / 2;
        int startY = (height - PANEL_HEIGHT) / 2;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, height / 2f, 0);
        ctx.getMatrices().scale(openAnim, openAnim, 1f);
        ctx.getMatrices().translate(-width / 2f, -height / 2f, 0);

        int idx = 0;
        for (Module.Category cat : CATS)
            renderPanel(ctx, startX + idx++ * (PANEL_WIDTH + PANEL_MARGIN), startY, cat, mx, my);

        Module hov = getHoveredModule(mx, my, startX, startY);
        if (hov != null && hov.getDescription() != null && !hov.getDescription().isEmpty())
            drawDescription(ctx, hov.getDescription(), startY);

        DescriptionRenderQueue.renderAll(ctx);
        renderSearchField(ctx);
        renderThemeButton(ctx, mx, my);
        renderTheme(ctx, mx, my);

        ctx.getMatrices().pop();
    }

    private void drawDescription(DrawContext ctx, String desc, int startY) {
        float dw = FontRenderers.sf_medium.getStringWidth(desc) + 14f;
        float dh = 20f;
        float dx = (width - dw) / 2f;
        float dy = startY - dh - 10f;
        Color guiColor = getGuiColor();
        ClickGui gui = ModuleManager.clickGui;
        if (gui != null && gui.blur.getValue()) {
            Render2DEngine.drawRoundedBlur(ctx.getMatrices(), dx - 6, dy - 3.5f, dw + 12, dh, 6f, guiColor);
        }
        Render2DEngine.drawRound(ctx.getMatrices(), dx, dy, dw, dh, 6f, guiColor);
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), desc, dx + 7, dy + 5, Color.WHITE.getRGB());
    }

    private void renderPanel(DrawContext ctx, int x, int y, Module.Category cat, int mx, int my) {
        Color guiColor = getGuiColor();
        ClickGui gui = ModuleManager.clickGui;
        if (gui != null && gui.blur.getValue()) {
            Render2DEngine.drawRoundedBlur(ctx.getMatrices(), x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_RADIUS, guiColor);
        }

        Render2DEngine.drawRound(ctx.getMatrices(), x - 1, y - 1, PANEL_WIDTH + 2, PANEL_HEIGHT + 2, PANEL_RADIUS + 1, PANEL_BORDER_COLOR);
        Render2DEngine.drawRound(ctx.getMatrices(), x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_RADIUS, PANEL_BG_COLOR);

        String title = cat.getName();
        String icon  = CAT_ICONS.getOrDefault(title, "");
        float titleW = FontRenderers.sf_bold.getStringWidth(title);
        float iconW  = FontRenderers.icons.getStringWidth(icon);
        int   titleX = (int)(x + (PANEL_WIDTH - titleW) / 2f);
        int   iconX  = (int)(titleX - iconW - 4);
        FontRenderers.sf_bold.drawString(ctx.getMatrices(), title, titleX, y + TITLE_MARGIN_TOP + 2, Color.WHITE.getRGB());
        if (!icon.isEmpty()) {
            FontRenderers.icons.drawString(ctx.getMatrices(), icon, iconX, y + TITLE_MARGIN_TOP + 2, Color.WHITE.getRGB());
        }

        int maxScroll = calcMaxScroll(cat);
        scrollTargets.put(cat, MathHelper.clamp(scrollTargets.get(cat), 0f, maxScroll));
        scrollOffsets.put(cat, MathHelper.clamp(scrollOffsets.get(cat), 0f, maxScroll));

        float offset = scrollOffsets.get(cat);
        renderScrollbar(ctx, x, y, cat, offset);

        ctx.getMatrices().push();
        Render2DEngine.addWindow(ctx.getMatrices(), x, y + SCROLL_AREA_Y, x + PANEL_WIDTH, y + SCROLL_AREA_Y + SCROLL_AREA_H, 1f);

        float curY = y + SCROLL_AREA_Y - offset;
        for (Module mod : Managers.MODULE.getModulesByCategory(cat)) {
            if (!isVisible(mod)) continue;

            float ep    = getExpandProg(mod);
            int   settH = (int)(computeSettH(mod) * ep);
            int   totalH = FUNCTION_HEIGHT + settH;

            if (curY + totalH < y + SCROLL_AREA_Y) { curY += totalH; continue; }
            if (curY > y + PANEL_HEIGHT) break;

            int col1 = mod.isOn() ? ColorUtil.getColorStyle(30)  : MODULE_OFF_COLOR.getRGB();
            int cm1  = mod.isOn() ? ColorUtil.getColorStyle(30,  MODULE_BG_ALPHA) : MODULE_OFF_BG_COLOR.getRGB();
            int cm2  = mod.isOn() ? ColorUtil.getColorStyle(120, MODULE_BG_ALPHA) : cm1;

            Render2DEngine.drawGradientRound(ctx.getMatrices(),
                x + 4, curY - 1, PANEL_WIDTH - 8, totalH - 1, MODULE_ROUNDING,
                new Color(cm2, true), new Color(cm2, true), new Color(cm2, true), new Color(cm2, true));

            String label = (functionBinding && bindingModule == mod) ? "Binding..." : mod.getName();
            if (mod.isOn()) {
                FontRenderers.sf_medium_modules.drawGradientString(ctx.getMatrices(), label, x + 10, curY + 3, 30);
            } else {
                FontRenderers.sf_medium_modules.drawString(ctx.getMatrices(), label, x + 10, curY + 3, col1);
            }

            if (settH > 0) {
                float setY = curY + FUNCTION_HEIGHT;
                ctx.getMatrices().push();
                Render2DEngine.addWindow(ctx.getMatrices(), x + 1, (int)setY, x + PANEL_WIDTH - 1, (int)(setY + settH), 1f);

                for (Setting<?> s : mod.getSettings()) {
                    if (!s.isVisible()) continue;
                    int sh = getSettH(s, PANEL_WIDTH - 20);
                    if (sh <= 0) continue;
                    if (isBool(s)) {
                        boolRend.render(ctx, s, x + 10, (int)setY, PANEL_WIDTH - 20, sh);
                    } else if (s.isNumberSetting() && s.hasRestriction()) {
                        sliderRend.render(ctx, s, x + 10, (int)setY - 2, PANEL_WIDTH - 20, sh);
                    } else if (s.isEnumSetting() && !s.isPositionSetting()) {
                        modeRend.render(ctx, s, x + 10, (int)setY, PANEL_WIDTH - 20, sh);
                    }
                    setY += sh + 1;
                }
                Render2DEngine.popWindow();
                ctx.getMatrices().pop();
            }

            if (!mod.getSettings().isEmpty()) {
                float ap = arrowProgress.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
                ap = lerp(ap, mod.isExpanded() ? 1f : 0f, 15f);
                if (Math.abs((mod.isExpanded() ? 1f : 0f) - ap) < 0.001f) ap = mod.isExpanded() ? 1f : 0f;
                arrowProgress.put(mod, ap);

                int ax = x + PANEL_WIDTH - 15;
                int ay = (int)(curY + FUNCTION_HEIGHT / 2f - 2);
                ctx.getMatrices().push();
                ctx.getMatrices().translate(ax, ay, 0);
                ctx.getMatrices().multiply(new Quaternionf().fromAxisAngleRad(new Vector3f(0, 0, 1), (float)Math.toRadians(90f * ap)));
                FontRenderers.sf_medium_modules.drawString(ctx.getMatrices(), "→", -4, -4, col1);
                ctx.getMatrices().pop();
            }

            curY += totalH;
        }

        Render2DEngine.popWindow();
        ctx.getMatrices().pop();
    }

    private void renderScrollbar(DrawContext ctx, int x, int y, Module.Category cat, float offset) {
        int max = calcMaxScroll(cat);
        if (max <= 0) return;
        int sbW = 3, sbX = x + PANEL_WIDTH - sbW - 1;
        int sbH = SCROLL_AREA_H - 30, sbY = y + SCROLL_AREA_Y + 15;
        Render2DEngine.drawRound(ctx.getMatrices(), sbX, sbY, sbW, sbH, 1f, SCROLLBAR_TRACK_COLOR);
        float prog = offset / max;
        int th = Math.max(6, (int)(sbH * (SCROLL_AREA_H / (float)(SCROLL_AREA_H + max))));
        int ty = sbY + (int)(prog * (sbH - th));
        Render2DEngine.drawRound(ctx.getMatrices(), sbX, ty, sbW, th, 1f, SCROLLBAR_THUMB_COLOR);
    }

    private void renderSearchField(DrawContext ctx) {
        Color guiColor = getGuiColor();
        int sw = SEARCH_MAX_WIDTH;
        int sx = (width - sw) / 2;
        int sy = (height + PANEL_HEIGHT) / 2 + SEARCH_MARGIN_BOT;
        ClickGui gui = ModuleManager.clickGui;
        if (gui != null && gui.blur.getValue()) {
            Render2DEngine.drawRoundedBlur(ctx.getMatrices(), sx, sy, sw, SEARCH_HEIGHT, 6f, guiColor);
        }
        Render2DEngine.drawRound(ctx.getMatrices(), sx, sy, sw, SEARCH_HEIGHT, 4f, guiColor);

        String disp; int col; float tx;
        if (searchState.text.isEmpty() && !searchState.focused) {
            disp = "Поиск...";
            col  = SEARCH_HINT_COLOR.getRGB();
            tx   = sx + (sw - FontRenderers.sf_medium.getStringWidth(disp)) / 2f;
        } else {
            String t = searchState.text;
            if (searchState.focused && searchState.cursorVisible) {
                int p = Math.min(searchState.cursorPosition, t.length());
                t = t.substring(0, p) + "|" + t.substring(p);
            }
            disp = t; col = Color.WHITE.getRGB(); tx = sx + 6;
        }
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), disp, tx,
            sy + (SEARCH_HEIGHT - 8) / 2f, col);
    }

    private void renderThemeButton(DrawContext ctx, int mx, int my) {
        Color guiColor = getGuiColor();
        int sw = SEARCH_MAX_WIDTH;
        int sx = (width - sw) / 2;
        int sy = (height + PANEL_HEIGHT) / 2 + SEARCH_MARGIN_BOT;
        int bx = sx + 185, by = sy + 2, bw = 16, bh = 16;
        boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        Render2DEngine.drawRound(ctx.getMatrices(), bx, by, bw, bh, 2f,
            hov ? guiColor.brighter() : guiColor);
        ctx.drawTexture(net.minecraft.util.Identifier.of("acoreaurora", "textures/gui/elements/colors2.png"),
            bx + 3, by + 2, 0, 0, 10, 10, 10, 10);
    }

    private void renderTheme(DrawContext ctx, int mx, int my) {
        if (themeAlphaAnim < 0.01f) return;
        Color guiColor = getGuiColor();
        int tw = THEME_MAX_WIDTH;
        int tx = (width - tw) / 2;
        int ty = (height + PANEL_HEIGHT) / 2 + THEME_MARGIN_BOT;
        float offY = (1f - themeMenuAnim) * 10f;

        int panelColor = ColorUtil.applyAlpha(guiColor.getRGB(), themeAlphaAnim);
        ClickGui gui = ModuleManager.clickGui;
        if (gui != null && gui.blur.getValue()) {
            Render2DEngine.drawRoundedBlur(ctx.getMatrices(), tx, ty + offY, tw, THEME_HEIGHT, 3f,
                new Color(panelColor, true));
        }
        Render2DEngine.drawRound(ctx.getMatrices(), tx, ty + offY, tw, THEME_HEIGHT, 3f,
            new Color(panelColor, true));

        int circleSize = THEME_HEIGHT - 5;
        int pad = 5;
        int totalThemes = ThemeManager.INSTANCE.getStyles().size() + 1;
        float maxScroll = Math.max(0, (totalThemes - VISIBLE_THEMES) * (circleSize + pad));
        themeScrollTarget = MathHelper.clamp(themeScrollTarget, 0, maxScroll);
        themeScrollOffset = MathHelper.clamp(themeScrollOffset, 0, maxScroll);

        ctx.getMatrices().push();
        Render2DEngine.addWindow(ctx.getMatrices(), tx + 1, (int)(ty + offY), tx + tw - 1, (int)(ty + offY + THEME_HEIGHT), 1f);

        float startX = tx + pad - themeScrollOffset;
        int centerY  = (int)(ty + (THEME_HEIGHT - circleSize) / 2f + 0.9f + offY);
        String hoveredTheme = null;

        int picX = (int)startX, picY = centerY;
        if (selectedColor1 != -1 && selectedColor2 != -1) {
            int vc1 = ColorUtil.applyAlpha(ThemeManager.gradient(5, 0,   selectedColor1, selectedColor2), themeAlphaAnim);
            int vc2 = ColorUtil.applyAlpha(ThemeManager.gradient(5, 180, selectedColor1, selectedColor2), themeAlphaAnim);
            int vc3 = ColorUtil.applyAlpha(ThemeManager.gradient(5, 90,  selectedColor1, selectedColor2), themeAlphaAnim);
            int vc4 = ColorUtil.applyAlpha(ThemeManager.gradient(5, 360, selectedColor1, selectedColor2), themeAlphaAnim);
            Render2DEngine.drawGradientRound(ctx.getMatrices(),
                picX, picY + 0.5f, circleSize, circleSize, 5f,
                new Color(vc1, true), new Color(vc2, true), new Color(vc3, true), new Color(vc4, true));
        } else {
            Render2DEngine.drawTexturedRect(ctx.getMatrices(),
                net.minecraft.util.Identifier.of("acoreaurora", "textures/gui/elements/pips.png"),
                picX, picY, circleSize, circleSize,
                ColorUtil.applyAlpha(Color.WHITE.getRGB(), themeAlphaAnim));
        }
        if (mx >= picX && mx <= picX + circleSize && my >= picY && my <= picY + circleSize) {
            Render2DEngine.drawRoundBorder(ctx.getMatrices(), picX - 1, picY - 0.5f, circleSize + 2, circleSize + 2, 5f, 0.5f,
                new Color(ColorUtil.applyAlpha(Color.WHITE.getRGB(), themeAlphaAnim), true));
            hoveredTheme = "ЛКМ - Создать свою тему";
        }
        startX += circleSize + pad;

        for (ThemeManager.Style style : ThemeManager.INSTANCE.getStyles()) {
            int sx = (int)startX;
            int c1 = style.colors[0];
            int c2 = style.colors.length > 1 ? style.colors[1] : c1;
            int vc1 = ColorUtil.applyAlpha(ThemeManager.gradient(5, 0,   c1, c2), themeAlphaAnim);
            int vc2 = ColorUtil.applyAlpha(ThemeManager.gradient(5, 180, c1, c2), themeAlphaAnim);
            int vc3 = ColorUtil.applyAlpha(ThemeManager.gradient(5, 90,  c1, c2), themeAlphaAnim);
            int vc4 = ColorUtil.applyAlpha(ThemeManager.gradient(5, 360, c1, c2), themeAlphaAnim);
            Render2DEngine.drawGradientRound(ctx.getMatrices(),
                sx, centerY + 0.5f, circleSize, circleSize, 5f,
                new Color(vc1, true), new Color(vc2, true), new Color(vc3, true), new Color(vc4, true));
            if (mx >= sx && mx <= sx + circleSize && my >= centerY && my <= centerY + circleSize) {
                hoveredTheme = style.name;
                Render2DEngine.drawRoundBorder(ctx.getMatrices(), sx - 1, centerY - 0.5f, circleSize + 2, circleSize + 2, 5f, 0.5f,
                    new Color(ColorUtil.applyAlpha(Color.WHITE.getRGB(), themeAlphaAnim), true));
            }
            startX += circleSize + pad;
        }

        Render2DEngine.popWindow();
        ctx.getMatrices().pop();

        if (colorPickerAnim > 0.01f) {
            int tx2 = (width - THEME_MAX_WIDTH) / 2;
            int ty2 = (height + PANEL_HEIGHT) / 2 + THEME_MARGIN_BOT;
            int fixedX = tx2 + pad;
            int fixedY = (int)(ty2 + (THEME_HEIGHT - circleSize) / 2f + 0.9f + offY);
            renderColorPicker(ctx, fixedX, fixedY);
        }

        if (hoveredTheme != null) themeNameAnim += (1f - themeNameAnim) * 0.2f;
        else                      themeNameAnim += (0f - themeNameAnim) * 0.2f;

        if (themeNameAnim > 0.01f && hoveredTheme != null) {
            float nw = FontRenderers.sf_medium.getStringWidth(hoveredTheme);
            float nx = (width - nw) / 2f;
            int centerY2 = (int)((height + PANEL_HEIGHT) / 2f + THEME_MARGIN_BOT
                + (THEME_HEIGHT - circleSize) / 2f + 0.9f + offY);
            FontRenderers.sf_medium.drawString(ctx.getMatrices(), hoveredTheme, nx,
                centerY2 + 18, ColorUtil.applyAlpha(Color.WHITE.getRGB(), themeNameAnim * themeAlphaAnim));
        }
    }

    private void renderColorPicker(DrawContext ctx, int x, int y) {
        Color guiColor = getGuiColor();
        int pw = 85, ph = 51;
        float animOffX  = (1f - colorPickerAnim) * 30f;
        float animScale = 0.95f + 0.05f * colorPickerAnim;
        float alpha     = colorPickerAnim;

        int panelX = (int)(x - pw - 20 + animOffX);
        int panelY = y - ph / 2 - 12;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(panelX + pw / 2f, panelY + ph / 2f, 0);
        ctx.getMatrices().scale(animScale, animScale, 1f);
        ctx.getMatrices().translate(-pw / 2f, -ph / 2f, 0);

        Render2DEngine.drawRound(ctx.getMatrices(), 0, 0, pw, ph, 4f,
            new Color(ColorUtil.applyAlpha(guiColor.getRGB(), alpha), true));

        net.minecraft.util.Identifier pickId =
            net.minecraft.util.Identifier.of("acoreaurora", "textures/gui/elements/pick.png");

        int p1s = 30, p1x = 5, p1y = 5;
        Render2DEngine.drawTexturedRect(ctx.getMatrices(), pickId, p1x, p1y, p1s, p1s,
            ColorUtil.applyAlpha(Color.WHITE.getRGB(), alpha));
        Render2DEngine.drawRoundBorder(ctx.getMatrices(), p1x, p1y, p1s, p1s, 14f, 0.1f,
            new Color(ColorUtil.applyAlpha(Color.WHITE.getRGB(), alpha), true));
        int dx1 = (int)(p1x + picker1CursorX * p1s), dy1 = (int)(p1y + picker1CursorY * p1s);
        Render2DEngine.renderRoundedQuad(ctx.getMatrices(), new Color(ColorUtil.applyAlpha(Color.BLACK.getRGB(), alpha), true),
            dx1 - 2, dy1 - 2, dx1 + 2, dy1 + 2, 2, 8);

        int p2s = 30, p2x = 50, p2y = 5;
        Render2DEngine.drawTexturedRect(ctx.getMatrices(), pickId, p2x, p2y, p2s, p2s,
            ColorUtil.applyAlpha(Color.WHITE.getRGB(), alpha));
        Render2DEngine.drawRoundBorder(ctx.getMatrices(), p2x, p2y, p2s, p2s, 14f, 0.1f,
            new Color(ColorUtil.applyAlpha(Color.WHITE.getRGB(), alpha), true));
        int dx2 = (int)(p2x + picker2CursorX * p2s), dy2 = (int)(p2y + picker2CursorY * p2s);
        Render2DEngine.renderRoundedQuad(ctx.getMatrices(), new Color(ColorUtil.applyAlpha(Color.BLACK.getRGB(), alpha), true),
            dx2 - 2, dy2 - 2, dx2 + 2, dy2 + 2, 2, 8);

        int cx = pw - 10, cy = 0;
        Render2DEngine.drawRound(ctx.getMatrices(), cx, cy, 10, 10, 2f,
            new Color(ColorUtil.applyAlpha(Color.WHITE.getRGB(), alpha), true));
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), "×", cx + 2, cy - 1.5f,
            ColorUtil.applyAlpha(Color.RED.getRGB(), alpha));

        int c1g = ColorUtil.getFirstColor(), c2g = ColorUtil.getSecondColor();
        Render2DEngine.drawGradientRound(ctx.getMatrices(), 14, 39, 56, 8, 1f,
            new Color(c1g, true), new Color(c1g, true), new Color(c2g, true), new Color(c2g, true));
        FontRenderers.sf_medium_modules.drawString(ctx.getMatrices(), "Добавить тему", 18, 40,
            ColorUtil.applyAlpha(Color.BLACK.getRGB(), alpha));

        ctx.getMatrices().pop();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int tw = THEME_MAX_WIDTH;
        int tx = (width - tw) / 2;
        int ty = (height + PANEL_HEIGHT) / 2 + THEME_MARGIN_BOT;
        if (mx >= tx && mx <= tx + tw && my >= ty && my <= ty + THEME_HEIGHT) {
            themeScrollTarget += (float)(-sy * THEME_SCROLL_SPEED);
            return true;
        }
        int totalW = CATS.size() * (PANEL_WIDTH + PANEL_MARGIN) - PANEL_MARGIN;
        int startX = (width - totalW) / 2;
        int startY = (height - PANEL_HEIGHT) / 2;
        int i = 0;
        for (Module.Category cat : CATS) {
            int px = startX + i++ * (PANEL_WIDTH + PANEL_MARGIN);
            if (mx >= px && mx <= px + PANEL_WIDTH
                    && my >= startY + SCROLL_AREA_Y && my <= startY + SCROLL_AREA_Y + SCROLL_AREA_H) {
                int max = calcMaxScroll(cat);
                if (max > 0) {
                    scrollTargets.compute(cat, (k, v) -> MathHelper.clamp(v - (float)sy * SCROLL_SPEED, 0, max));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int totalW = CATS.size() * (PANEL_WIDTH + PANEL_MARGIN) - PANEL_MARGIN;
        int startX = (width - totalW) / 2;
        int startY = (height - PANEL_HEIGHT) / 2;
        int sw = SEARCH_MAX_WIDTH, searchX = (width - sw) / 2;
        int searchY = (height + PANEL_HEIGHT) / 2 + SEARCH_MARGIN_BOT;
        int bx = searchX + 185, by = searchY + 2, bw2 = 16, bh2 = 16;

        if (colorPickerOpen) {
            int tx2 = (width - THEME_MAX_WIDTH) / 2;
            int ty2 = (height + PANEL_HEIGHT) / 2 + THEME_MARGIN_BOT;
            int circleSize = THEME_HEIGHT - 5, pad = 5;
            float offY = (1f - themeMenuAnim) * 10f;
            int fixedX = tx2 + pad;
            int fixedY = (int)(ty2 + (THEME_HEIGHT - circleSize) / 2f + 0.9f + offY);
            int pw = 85, ph = 51;
            int panelX = fixedX - pw - 20;
            int panelY = fixedY - ph / 2 - 12;

            int closeBX = panelX + pw - 10, closeBY = panelY;
            if (mx >= closeBX && mx <= closeBX + 10 && my >= closeBY && my <= closeBY + 10) {
                colorPickerOpen = false; return true;
            }
            int p1x = panelX + 5, p1y = panelY + 5, p1s = 30;
            if (mx >= p1x && mx <= p1x + p1s && my >= p1y && my <= p1y + p1s) {
                float u = (float)(mx - p1x) / p1s, v = (float)(my - p1y) / p1s;
                picker1CursorX = MathHelper.clamp(u, 0, 1);
                picker1CursorY = MathHelper.clamp(v, 0, 1);
                selectedColor1 = getPixelColor(picker1CursorX, picker1CursorY);
                draggingPicker1 = true; return true;
            }
            int p2x = panelX + 50, p2y = panelY + 5, p2s = 30;
            if (mx >= p2x && mx <= p2x + p2s && my >= p2y && my <= p2y + p2s) {
                float u = (float)(mx - p2x) / p2s, v = (float)(my - p2y) / p2s;
                picker2CursorX = MathHelper.clamp(u, 0, 1);
                picker2CursorY = MathHelper.clamp(v, 0, 1);
                selectedColor2 = getPixelColor(picker2CursorX, picker2CursorY);
                draggingPicker2 = true; return true;
            }
            int addX = panelX + 14, addY = panelY + 39, addW = 56, addH = 8;
            if (mx >= addX && mx <= addX + addW && my >= addY && my <= addY + addH) {
                String base = "Custom"; String cand = base; int ci = 2;
                boolean ex;
                do {
                    ex = false;
                    for (ThemeManager.Style s : ThemeManager.INSTANCE.getStyles()) {
                        if (s.name.equals(cand)) { ex = true; cand = base + "-" + ci++; break; }
                    }
                } while (ex);
                ThemeManager.INSTANCE.addCustom(cand, selectedColor1, selectedColor2);
                final String finalCand = cand;
                ThemeManager.INSTANCE.setTheme(
                    ThemeManager.INSTANCE.getStyles().stream()
                        .filter(s -> s.name.equals(finalCand)).findFirst().orElse(null));
                colorPickerOpen = false; return true;
            }
        }

        if (mx >= searchX && mx <= searchX + sw && my >= searchY && my <= searchY + SEARCH_HEIGHT) {
            searchState.focused = true;
            searchState.cursorPosition = searchState.text.length();
            return true;
        } else {
            searchState.focused = false;
        }

        if (mx >= bx && mx <= bx + bw2 && my >= by && my <= by + bh2) {
            themeMenu = !themeMenu;
            themeMenuTarget = themeMenu ? 1f : 0f;
            return true;
        }

        if (themeMenu) {
            int ttx = (width - THEME_MAX_WIDTH) / 2;
            int tty = (height + PANEL_HEIGHT) / 2 + THEME_MARGIN_BOT;
            int cs = THEME_HEIGHT - 5, pad = 5;
            int centerY = tty + (THEME_HEIGHT - cs) / 2;
            float curX = ttx + pad - themeScrollTarget;

            if (mx >= curX && mx <= curX + cs && my >= centerY && my <= centerY + cs) {
                colorPickerOpen = !colorPickerOpen; return true;
            }
            curX += cs + pad;

            if (btn == 0) {
                for (ThemeManager.Style style : ThemeManager.INSTANCE.getStyles()) {
                    if (mx >= curX && mx <= curX + cs && my >= centerY && my <= centerY + cs) {
                        ThemeManager.INSTANCE.setTheme(style); return true;
                    }
                    curX += cs + pad;
                }
            }
            if (btn == 1) {
                curX = ttx + pad + cs + pad - themeScrollTarget;
                for (ThemeManager.Style style : new ArrayList<>(ThemeManager.INSTANCE.getStyles())) {
                    if (mx >= curX && mx <= curX + cs && my >= centerY && my <= centerY + cs) {
                        if (style.name.startsWith("Custom")) ThemeManager.INSTANCE.removeStyle(style);
                        return true;
                    }
                    curX += cs + pad;
                }
            }
        }

        if (functionBinding && bindingModule != null) {
            bindingModule.setBind(-(btn + 2), true, false);
            functionBinding = false; bindingModule = null; return true;
        }

        int idx = 0;
        for (Module.Category cat : CATS) {
            int px = startX + idx++ * (PANEL_WIDTH + PANEL_MARGIN);
            float off = scrollOffsets.get(cat);
            float curY = startY + SCROLL_AREA_Y - off;

            for (Module mod : Managers.MODULE.getModulesByCategory(cat)) {
                if (!isVisible(mod)) continue;
                float ep    = expandProgress.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
                int   settH = (int)(computeSettH(mod) * ep);
                int   totalH = FUNCTION_HEIGHT + settH;

                if (curY + totalH < startY + SCROLL_AREA_Y) { curY += totalH; continue; }
                if (curY > startY + SCROLL_AREA_Y + SCROLL_AREA_H) break;

                if (mx >= px && mx <= px + PANEL_WIDTH
                        && my >= curY && my <= curY + FUNCTION_HEIGHT
                        && my >= startY + SCROLL_AREA_Y && my <= startY + SCROLL_AREA_Y + SCROLL_AREA_H) {
                    if (btn == 0)      { mod.toggle(); return true; }
                    else if (btn == 1) { mod.setExpanded(!mod.isExpanded()); return true; }
                    else if (btn == 2) { functionBinding = true; bindingModule = mod; return true; }
                }

                if (settH > 0) {
                    float setY = curY + FUNCTION_HEIGHT;
                    for (Setting<?> s : mod.getSettings()) {
                        if (!s.isVisible()) continue;
                        int sh = getSettH(s, PANEL_WIDTH - 20);
                        if (sh <= 0) continue;
                        if (mx >= px && mx <= px + PANEL_WIDTH
                                && my >= setY && my <= setY + sh
                                && my >= startY + SCROLL_AREA_Y && my <= startY + SCROLL_AREA_Y + SCROLL_AREA_H) {
                            if (isBool(s)) {
                                boolRend.mouseClicked(s, mx, my, btn, px + 10, (int)setY, PANEL_WIDTH - 20, sh);
                            } else if (s.isNumberSetting() && s.hasRestriction()) {
                                if (sliderRend.mouseClicked(s, mx, my, btn, px + 10, (int)setY - 2, PANEL_WIDTH - 20, sh)) {
                                    draggingSlider = s;
                                    draggingSliderX = px + 10;
                                    draggingSliderW = PANEL_WIDTH - 20;
                                }
                            } else if (s.isEnumSetting() && !s.isPositionSetting()) {
                                modeRend.mouseClicked(s, mx, my, btn, px + 10, (int)setY, PANEL_WIDTH - 20, sh);
                            }
                        }
                        setY += sh + 1;
                    }
                }
                curY += totalH;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (btn == 0 && colorPickerOpen) {
            int tx2 = (width - THEME_MAX_WIDTH) / 2;
            int ty2 = (height + PANEL_HEIGHT) / 2 + THEME_MARGIN_BOT;
            int cs = THEME_HEIGHT - 5, pad = 5;
            float offY = (1f - themeMenuAnim) * 10f;
            int fixedX = tx2 + pad;
            int fixedY = (int)(ty2 + (THEME_HEIGHT - cs) / 2f + 0.9f + offY);
            int pw = 85, ph = 51;
            int panelX = fixedX - pw - 20, panelY = fixedY - ph / 2 - 12;

            if (draggingPicker1) {
                float u = (float)(mx - panelX - 5) / 30f, v = (float)(my - panelY - 5) / 30f;
                picker1CursorX = MathHelper.clamp(u, 0, 1);
                picker1CursorY = MathHelper.clamp(v, 0, 1);
                selectedColor1 = getPixelColor(picker1CursorX, picker1CursorY);
                return true;
            }
            if (draggingPicker2) {
                float u = (float)(mx - panelX - 50) / 30f, v = (float)(my - panelY - 5) / 30f;
                picker2CursorX = MathHelper.clamp(u, 0, 1);
                picker2CursorY = MathHelper.clamp(v, 0, 1);
                selectedColor2 = getPixelColor(picker2CursorX, picker2CursorY);
                return true;
            }
        }
        if (draggingSlider != null && btn == 0) {
            sliderRend.mouseDragged(draggingSlider, mx, draggingSliderX, draggingSliderW);
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        draggingPicker1 = false;
        draggingPicker2 = false;
        if (draggingSlider != null) {
            sliderRend.mouseReleased(draggingSlider);
            draggingSlider = null;
            return true;
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean charTyped(char c, int kc) {
        if (searchState.focused && searchState.text.length() < 30) {
            String b = searchState.text.substring(0, searchState.cursorPosition);
            String a = searchState.text.substring(searchState.cursorPosition);
            searchState.text = b + c + a;
            searchState.cursorPosition++;
            resetScroll();
            return true;
        }
        return super.charTyped(c, kc);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mods) {
        if (functionBinding && bindingModule != null) {
            if (kc == GLFW.GLFW_KEY_ESCAPE || kc == GLFW.GLFW_KEY_DELETE)
                bindingModule.setBind(-1, false, false);
            else
                bindingModule.setBind(kc, false, false);
            functionBinding = false; bindingModule = null; return true;
        }
        if (searchState.focused) {
            switch (kc) {
                case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER -> { searchState.focused = false; return true; }
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (searchState.cursorPosition > 0) {
                        searchState.text = searchState.text.substring(0, searchState.cursorPosition - 1)
                            + searchState.text.substring(searchState.cursorPosition);
                        searchState.cursorPosition--;
                        resetScroll();
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    if (searchState.cursorPosition < searchState.text.length()) {
                        searchState.text = searchState.text.substring(0, searchState.cursorPosition)
                            + searchState.text.substring(searchState.cursorPosition + 1);
                        resetScroll();
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT  -> { if (searchState.cursorPosition > 0) searchState.cursorPosition--; return true; }
                case GLFW.GLFW_KEY_RIGHT -> { if (searchState.cursorPosition < searchState.text.length()) searchState.cursorPosition++; return true; }
            }
        }
        if (kc == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(kc, sc, mods);
    }

    @Override
    public void close() {
        if (isClose) return;
        draggingPicker1 = false;
        draggingPicker2 = false;
        isClose = true;
    }

    @Override
    public void tick() {
        super.tick();
        long now = System.currentTimeMillis();
        if (now - searchState.lastCursorBlink >= 500) {
            searchState.cursorVisible = !searchState.cursorVisible;
            searchState.lastCursorBlink = now;
        }
    }

    private Color getGuiColor() {
        return GUI_COLOR;
    }

    private boolean isVisible(Module m) {
        String f = searchState.text.toLowerCase();
        return f.isEmpty() || m.getName().toLowerCase().contains(f);
    }

    private int calcMaxScroll(Module.Category cat) {
        int total = 0;
        for (Module m : Managers.MODULE.getModulesByCategory(cat)) {
            if (!isVisible(m)) continue;
            float ep = expandProgress.getOrDefault(m, m.isExpanded() ? 1f : 0f);
            total += FUNCTION_HEIGHT + (int)(computeSettH(m) * ep);
        }
        return Math.max(0, total - SCROLL_AREA_H);
    }

    private float getExpandProg(Module m) {
        float target = m.isExpanded() ? 1f : 0f;
        float prog   = expandProgress.getOrDefault(m, target);
        prog = lerp(prog, target, 15f);
        if (Math.abs(target - prog) < 0.001f) prog = target;
        expandProgress.put(m, prog);
        return prog;
    }

    private int computeSettH(Module m) {
        int h = 0;
        for (Setting<?> s : m.getSettings()) {
            if (!s.isVisible()) continue;
            h += getSettH(s, PANEL_WIDTH - 20) + 1;
        }
        return Math.max(0, h);
    }

    private int getSettH(Setting<?> s, int w) {
        if (!s.isVisible()) return 0;
        if (isBool(s)) return boolRend.getHeight();
        if (s.isNumberSetting() && s.hasRestriction()) return sliderRend.getHeight();
        if (s.isEnumSetting() && !s.isPositionSetting()) return modeRend.getHeight(s, w);
        return 0;
    }

    private boolean isBool(Setting<?> s) {
        return s.getValue() instanceof Boolean
            && !s.getName().equals("Enabled")
            && !s.getName().equals("Drawn");
    }

    private void resetScroll() {
        CATS.forEach(c -> { scrollTargets.put(c, 0f); scrollOffsets.put(c, 0f); });
    }

    private Module getHoveredModule(int mx, int my, int startX, int startY) {
        int idx = 0;
        for (Module.Category cat : CATS) {
            int px = startX + idx++ * (PANEL_WIDTH + PANEL_MARGIN);
            if (mx < px || mx > px + PANEL_WIDTH) continue;
            float off  = scrollOffsets.get(cat);
            float curY = startY + SCROLL_AREA_Y - off;
            for (Module m : Managers.MODULE.getModulesByCategory(cat)) {
                if (!isVisible(m)) continue;
                float ep     = expandProgress.getOrDefault(m, m.isExpanded() ? 1f : 0f);
                int   totalH = FUNCTION_HEIGHT + (int)(computeSettH(m) * ep);
                if (mx >= px && mx <= px + PANEL_WIDTH
                        && my >= curY && my <= curY + FUNCTION_HEIGHT
                        && my >= startY + SCROLL_AREA_Y
                        && my <= startY + SCROLL_AREA_Y + SCROLL_AREA_H)
                    return m;
                curY += totalH;
            }
        }
        return null;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(1f, t * 0.05f);
    }
}

class SearchState {
    String text = "";
    boolean focused = false;
    int cursorPosition = 0;
    long lastCursorBlink = System.currentTimeMillis();
    boolean cursorVisible = true;
}
