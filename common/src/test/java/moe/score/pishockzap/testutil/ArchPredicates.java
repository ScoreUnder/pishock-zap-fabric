package moe.score.pishockzap.testutil;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

public class ArchPredicates {
    public static DescribedPredicate<JavaClass> haveOrWithinFullyQualifiedName(String name) {
        return new DescribedPredicate<>("have name or be enclosed with a class with name " + name) {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getFullName().equals(name) || name.equals(javaClass.getEnclosingClass().map(JavaClass::getFullName).orElse(null));
            }
        };
    }
}
