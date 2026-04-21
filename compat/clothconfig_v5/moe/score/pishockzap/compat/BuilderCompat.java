package moe.score.pishockzap.compat;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
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

        public SubCategoryBuilderCompat(SubCategoryBuilder builder) {
            this.builder = builder;
        }

        public @NotNull SubCategoryListEntry build() {
            return builder.build();
        }

        public SubCategoryBuilderCompat setExpanded(boolean expanded) {
            builder.setExpanded(expanded);
            return this;
        }

        public SubCategoryBuilderCompat setDisplayRequirement(BooleanSupplier requirement) {
            return this;
        }

        public boolean add(AbstractConfigListEntry<?> abstractConfigListEntry) {
            return builder.add(abstractConfigListEntry);
        }
    }
}
