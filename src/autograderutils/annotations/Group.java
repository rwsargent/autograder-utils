package autograderutils.annotations;

import java.lang.annotation.Repeatable;

@Repeatable(Groups.class)
public @interface Group {
	String name();
	int totalPoints();
}
