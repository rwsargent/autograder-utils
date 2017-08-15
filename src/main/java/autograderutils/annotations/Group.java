package autograderutils.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Repeatable(Groups.class)
@Retention( RetentionPolicy.RUNTIME )
public @interface Group {
	String name();
	int totalPoints();
}
