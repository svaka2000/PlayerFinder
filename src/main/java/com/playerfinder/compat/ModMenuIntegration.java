package com.playerfinder.compat;

import com.playerfinder.PlayerFinder;
import com.playerfinder.gui.FinderScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Adds a "Configure" button for PlayerFinder to the Mod Menu list.
 *
 * <p>Mod Menu is an optional dependency: this entrypoint only runs when Mod Menu is installed, and the
 * mod is compiled against its API with {@code modCompileOnly} (never bundled).
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new FinderScreen(parent, PlayerFinder.config(), () -> {
            PlayerFinder.save();
            PlayerFinder.rebuild();
        });
    }
}
