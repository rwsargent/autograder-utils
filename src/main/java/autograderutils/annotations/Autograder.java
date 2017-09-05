package autograderutils.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The Autograder annotation is used to decorate JUnit tests with point values associate them
 * with a certain group.
 * @author ryans
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Autograder {
	int pointsPossible() default 1;
	String group() default "Assignment";
	int pointsOnFailure() default 0;
}
