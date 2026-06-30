package com.playerfinder.mixin;

import com.playerfinder.PlayerFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "Hide everyone but them" (solo): when a solo group is active, tell the renderer not to draw other
 * players who aren't in that group. This only suppresses the local client's own rendering of those
 * players — it removes information from your view rather than adding any, sends no packets, and never
 * touches positions or hitboxes. Your own model and all non-player entities are always drawn.
 * (Render-only.)
 */
@Mixin(EntityRenderer.class)
public abstract class EntityCullMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void playerfinder$soloCull(Entity entity, Frustum frustum, double camX, double camY, double camZ,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!PlayerFinder.isSoloActive()) return;
        if (!(entity instanceof AbstractClientPlayer)) return;
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player) return;   // never hide yourself
        if (PlayerFinder.isHiddenBySolo(entity.getUUID(), entity.getName().getString())) {
            cir.setReturnValue(false);
        }
    }
}
