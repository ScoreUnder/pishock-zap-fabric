import net.fabricmc.loom.api.mappings.layered.MappingContext;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.api.mappings.layered.spec.FileSpec;
import net.fabricmc.loom.api.mappings.layered.spec.MappingsSpec;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("UnstableApiUsage")
public record PostProcessingMappingSpec(FileSpec fileSpec, String sourceNamespace)
    implements MappingsSpec<PostProcessingMappingLayer> {
    @Override
    public PostProcessingMappingLayer createLayer(@NonNull MappingContext context) {
        return new PostProcessingMappingLayer(fileSpec.get(context), MappingsNamespace.of(sourceNamespace));
    }
}
