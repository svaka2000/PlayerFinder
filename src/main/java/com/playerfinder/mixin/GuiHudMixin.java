package com.playerfinder.mixin;

import com.playerfinder.hud.FinderHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draw the PlayerFinder "who's online" panel on top of the in-game HUD. (Render-only HUD overlay.) */
@Mixin(Gui.class)
public abstract class GuiHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void playerfinder$renderHud(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        FinderHud.render(graphics);
    }
}
