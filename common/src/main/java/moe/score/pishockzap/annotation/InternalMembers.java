package moe.score.pishockzap.annotation;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated class is for internal use only, though it may need to be passed around in public APIs.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@ApiStatus.Internal
public @interface InternalMembers {
}
