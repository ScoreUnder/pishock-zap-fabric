package moe.score.pishockzap.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import lombok.NonNull;
import moe.score.pishockzap.PishockZapModConfigMenu;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ModMenuImpl implements ModMenuApi {
    @Override
    public @NonNull ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PishockZapModConfigMenu::createConfigScreen;
    }
}
