package moe.score.pishockzap.compat.clothconfig;

import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.Supplier;

@UtilityClass
public class ClothUtil {
    public static Supplier<Optional<Component[]>> supply(Component... tooltip) {
        var option = Optional.of(tooltip);
        return () -> option;
    }
}
