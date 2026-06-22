package acore.aurora.injection;

import acore.aurora.AcoreAurora;
import acore.aurora.core.Managers;
import acore.aurora.events.impl.EventKeyPress;
import acore.aurora.events.impl.EventKeyRelease;
import acore.aurora.gui.clickui.ClickGUI;
import acore.aurora.gui.hud.HudEditorGui;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import acore.aurora.features.modules.Module;

import static acore.aurora.features.modules.Module.mc;

@Mixin(Keyboard.class)
public class MixinKeyboard {

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if(Module.fullNullCheck()) return;
        boolean whitelist = mc.currentScreen == null || mc.currentScreen instanceof ClickGUI || mc.currentScreen instanceof HudEditorGui;
        if (!whitelist) return;

        if (action == 0) Managers.MODULE.onKeyReleased(key);
        if (action == 1) Managers.MODULE.onKeyPressed(key);
        if (action == 2) action = 1;

        switch (action) {
            case 0 -> {
                EventKeyRelease event = new EventKeyRelease(key, scanCode);
                AcoreAurora.EVENT_BUS.post(event);
                if (event.isCancelled()) ci.cancel();
            }
            case 1 -> {
                EventKeyPress event = new EventKeyPress(key, scanCode);
                AcoreAurora.EVENT_BUS.post(event);
                if (event.isCancelled()) ci.cancel();
            }
        }
    }
}