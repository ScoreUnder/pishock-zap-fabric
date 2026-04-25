import net.fabricmc.loom.api.mappings.layered.MappingLayer;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.MappingDstNsReorder;
import net.fabricmc.mappingio.adapter.MappingNsCompleter;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public record PostProcessingMappingLayer(Path path, MappingsNamespace sourceNamespace) implements MappingLayer {
    @Override
    public void visit(@NonNull MappingVisitor visitor) throws IOException {
        MemoryMappingTree tree = (MemoryMappingTree) visitor;

        MemoryMappingTree renamedTree = new MemoryMappingTree();
        tree.accept(new MappingNsCompleter(renamedTree, Map.of("previous", "named"), true));
        MemoryMappingTree workTree = new MemoryMappingTree();
        renamedTree.accept(new MappingSourceNsSwitch(workTree, "previous"));
        renamedTree = null;

        MappingReader.read(path, workTree);

        MemoryMappingTree switchedTree = new MemoryMappingTree();

        String sourceNamespaceName = getSourceNamespace().toString();
        workTree.accept(new MappingSourceNsSwitch(switchedTree, sourceNamespaceName));
        workTree = null;

        switchedTree.accept(new MappingDstNsReorder(tree, List.of(sourceNamespaceName, "named")));
    }

    @Override
    @NotNull
    public MappingsNamespace getSourceNamespace() {
        return sourceNamespace;
    }
}
