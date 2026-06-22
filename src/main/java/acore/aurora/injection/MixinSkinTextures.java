package acore.aurora.injection;

import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import acore.aurora.core.manager.client.ModuleManager;
import acore.aurora.features.modules.misc.NameProtect;
import acore.aurora.utility.render.TextureStorage;

@Mixin(SkinTextures.class)
public class MixinSkinTextures {
    @Inject(method = "texture", at = @At("HEAD"), cancellable = true)
    public void getSkinTextureHook(CallbackInfoReturnable<Identifier> cir) {
        if (ModuleManager.nameProtect.isEnabled()) {
            cir.setReturnValue(TextureStorage.sunRiseSkin);
        }
    }
}
