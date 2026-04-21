package moe.score.pishockzap.compat;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import moe.score.pishockzap.compat.mixin.pool.HasDisplayRequirement;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public class BuilderCompat {
    public static SubCategoryBuilderCompat of(SubCategoryBuilder builder) {
        return new SubCategoryBuilderCompat(builder);
    }

    public static SubCategoryBuilderCompat subCategory(ConfigEntryBuilder builder, Component title) {
        return of(builder.startSubCategory(title));
    }

    public static class SubCategoryBuilderCompat {
        private final SubCategoryBuilder builder;
        private BooleanSupplier displayRequirement = () -> true;

        public SubCategoryBuilderCompat(SubCategoryBuilder builder) {
            this.builder = builder;
        }

        public @NotNull SubCategoryListEntry build() {
            var result = builder.build();
            if (result instanceof HasDisplayRequirement displayReq) {
                displayReq.pishockzap$setDisplayRequirement(displayRequirement);
            }
            return result;
        }

        public SubCategoryBuilderCompat setExpanded(boolean expanded) {
            builder.setExpanded(expanded);
            return this;
        }

        public SubCategoryBuilderCompat setDisplayRequirement(BooleanSupplier requirement) {
            this.displayRequirement = requirement;
            return this;
        }

        public boolean add(AbstractConfigListEntry<?> abstractConfigListEntry) {
            return builder.add(abstractConfigListEntry);
        }
    }
}
