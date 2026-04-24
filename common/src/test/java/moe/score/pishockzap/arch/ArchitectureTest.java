package moe.score.pishockzap.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static moe.score.pishockzap.testutil.ArchPredicates.haveOrWithinFullyQualifiedName;

class ArchitectureTest {
    private static final JavaClasses classes = new ClassFileImporter()
        .importPackages("moe.score.pishockzap");

    /**
     * MC, Fabric, Cloth Config, and ModMenu APIs should only appear in:
     * <ul>
     *   <li>{@code moe.score.pishockzap.compat.**} (compat layer)</li>
     *   <li>{@code moe.score.pishockzap.mixin.**} (mixins — necessarily MC-coupled)</li>
     *   <li>{@code PishockZapMod} (Fabric entry point)</li>
     * </ul>
     */
    @Test
    void externalMinecraftApiOnlyInCompatAndMod() {
        noClasses()
            .that().resideInAPackage("moe.score.pishockzap..")
            .and().resideOutsideOfPackages(
                "moe.score.pishockzap.mixin..",
                "moe.score.pishockzap.compat.."
            )
            .and().doNotHaveFullyQualifiedName("moe.score.pishockzap.PishockZapMod")
            .and().doNotHaveFullyQualifiedName("moe.score.pishockzap.PlayerHpWatcher")
            .and(not(haveOrWithinFullyQualifiedName("moe.score.pishockzap.PishockZapModConfigMenu")))
            .should().dependOnClassesThat().resideInAnyPackage(
                "net.minecraft..",
                "net.fabricmc..",
                "me.shedaniel..",
                "com.terraformersmc..",
                "com.mojang.."
            )
            .check(classes);
    }
}
