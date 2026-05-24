package net.pemiridosa.combind;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.pemiridosa.combind.config.CombindConfigScreen;

public class CombindModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CombindConfigScreen::new;
    }
}
