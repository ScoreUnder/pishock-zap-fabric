package moe.score.pishockzap.arch;

import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

class AbiStabilityTest {
    private static final String INTERNAL_ANNOTATION = "org.jetbrains.annotations.ApiStatus$Internal";
    private static final String MIXIN_ANNOTATION = "org.spongepowered.asm.mixin.Mixin";

    private static @NonNull TreeSet<String> getPublicFacingMembers() {
        var myClasses = new ClassFileImporter()
            .importPackages("moe.score.pishockzap")
            .stream()
            .filter(c -> !c.getName().endsWith("Test") && !c.getName().contains("Test$") && !c.getPackageName().contains(".test"))
            .filter(c -> isAccessibleFromOutside(c) && !isInternal(c))
            .toList();
        var classSignatures = myClasses.stream()
            .map(AbiStabilityTest::signature);
        var members = myClasses.stream()
            .flatMap(c -> c.getMembers().stream())
            .filter(m -> isAccessibleFromOutside(m) && !isInternal(m))
            .map(AbiStabilityTest::signature);
        return Stream.concat(classSignatures, members).collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean isInternal(JavaMember member) {
        return member.isAnnotatedWith(INTERNAL_ANNOTATION);
    }

    private static boolean isInternal(JavaClass clazz) {
        return clazz.isAnnotatedWith(INTERNAL_ANNOTATION) || clazz.getPackage().isAnnotatedWith(INTERNAL_ANNOTATION)
            || clazz.isAnnotatedWith(MIXIN_ANNOTATION)
            || clazz.getEnclosingClass().map(AbiStabilityTest::isInternal).orElse(false);
    }

    private static boolean isAccessibleFromOutside(HasModifiers m) {
        return m.getModifiers().contains(JavaModifier.PUBLIC) || m.getModifiers().contains(JavaModifier.PROTECTED);
    }

    private static String signature(JavaMember member) {
        var tag = member instanceof JavaCodeUnit ? "M" : member instanceof JavaField ? "F" : "?";
        return tag + ":" + member.getOwner().getName() + ":" + member.getName() + ":" + getModifiersStr(member) + ":" + member.getDescriptor();
    }

    private static @NonNull String getModifiersStr(HasModifiers m) {
        return m.getModifiers().stream().map(JavaModifier::name).sorted().collect(Collectors.joining(","));
    }

    private static String signature(JavaClass clazz) {
        var modifiers = getModifiersStr(clazz);
        var inheritance = Stream.concat(
            clazz.getSuperclass().map(JavaType::getName).filter(n -> !n.equals("java.lang.Object")).stream(),
            clazz.getInterfaces().stream().map(JavaType::getName).sorted()
        ).collect(Collectors.joining(","));
        return "C:" + clazz.getFullName() + genericsToString(clazz.getTypeParameters()) + ":" + modifiers + ":" + inheritance;
    }

    private static <T extends HasDescription> String genericsToString(List<JavaTypeVariable<T>> param) {
        if (param.isEmpty()) return "";
        var sb = new StringBuilder();
        sb.append('<');
        var first = true;
        for (var typeVar : param) {
            if (!first) sb.append(',');
            else first = false;
            sb.append(typeVar.getName());
            var bounds = typeVar.getUpperBounds();
            if (!bounds.isEmpty() && !(bounds.size() == 1 && bounds.get(0).getName().equals("java.lang.Object"))) {
                sb.append(" extends ");
                sb.append(bounds.stream().map(JavaType::getName).collect(Collectors.joining("&")));
            }
        }
        sb.append('>');
        return sb.toString();
    }

    private static Set<String> loadExpected() throws IOException {
        var stream = AbiStabilityTest.class.getResourceAsStream("/abi-signatures.txt");
        if (stream == null) return null;
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    @Test
    void publicAbiMatchesSignatureFile() throws IOException {
        var expected = loadExpected();
        if (expected == null) {
            fail("abi-signatures.txt not found in test resources.");
            return;
        }

        var actual = getPublicFacingMembers();

        var added = difference(actual, expected);
        var removed = difference(expected, actual);

        var sb = new StringBuilder();
        if (!added.isEmpty()) {
            sb.append("Methods have been added to ABI:\n");
            sb.append(String.join("\n", added));
        }
        if (!removed.isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n\n");
            sb.append("Methods have been removed from ABI:\n");
            sb.append(String.join("\n", removed));
        }
        if (!sb.isEmpty()) fail(sb.toString());
    }

    private static <V> TreeSet<V> difference(Set<V> a, Set<V> b) {
        TreeSet<V> result = new TreeSet<>(a);
        result.removeAll(b);
        return result;
    }
}
