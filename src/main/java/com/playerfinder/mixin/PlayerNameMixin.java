package com.playerfinder.mixin;

import com.playerfinder.PlayerFinder;
import com.playerfinder.compat.ProfileCompat;
import com.playerfinder.core.Txt;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Highlight: recolour a player's display name to its group colour. This is the same display name the
 * client uses for the floating nametag, the tab list and chat — so a grouped player stands out wherever
 * their name appears. Purely cosmetic and occlusion-natural: it changes only the colour of a name you
 * would already see, and reveals nothing through walls. (Render-only.)
 */
@Mixin(Player.class)
public abstract class PlayerNameMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void playerfinder$highlightName(CallbackInfoReturnable<Component> cir) {
        Player self = (Player) (Object) this;
        String profileName = ProfileCompat.name(self.getGameProfile());
        int rgb = PlayerFinder.highlightRgbFor(self.getUUID(), profileName);
        if (rgb < 0) return;

        Component original = cir.getReturnValue();
        String text = (original != null) ? original.getString() : profileName;
        if (text == null) return;
        cir.setReturnValue(Txt.colored(text, rgb));
    }
}
