package autograderutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Autograder annotation is used to decorate JUnit tests with point values associate them
 * with a certain group.
 * @author ryans
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.METHOD)
public @interface Autograder {
	int pointsPossible() default 1;
	String group() default "Assignment";
	int pointsOnFailure() default 0;
}
