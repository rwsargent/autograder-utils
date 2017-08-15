package autograderutils;


import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import autograderutils.annotations.Autograder;
import autograderutils.annotations.Group;
import autograderutils.annotations.Groups;

/**
 * Used to run a JUnit test suite and pipe its results to stdout. 
 * These path arguments must be absolute due to the nature of the Autograder grading portion.
 * 
 * group.properties is a properties file of groupName=pointsPossible, that the Autograder annotation will key off of.
 * An example might look like this:
 * mergesort=20
 * quicksort=20
 * insertion_sort=20
 * 
 * If no groups are neccessary, the second argument needs to be the number of total possible pints in the grader script. 
 * The @Autograder annotation will default to "Autograder," but the extra.properites file is always necessary.
 *
 * The extra.properties file is a similar key=value pair where the key is category that isn't tested by JUnit, i.e.
 * test=10
 * style=10
 * analysis=30
 * 
 * @author ryans
 *
 */
public class JUnitPlugin {
	public AutograderResult grade(Class<?> junitGradingClass) {
		// Grab the toal number of points from 
		Map<String, Integer> totalGroupPoints = getGroups(junitGradingClass);
		
		// zero out points
		Map<String, Integer> scorePerGroup = new HashMap<>(totalGroupPoints);
		for( Entry<String, Integer> entry : scorePerGroup.entrySet()) {
			entry.setValue(0);
		}
		
		JUnitCore core = new JUnitCore();
		core.addListener(new RunListener());
		Result junitResult = core.run(junitGradingClass);
		AutograderResult autoResult = new AutograderResult(totalGroupPoints);
		
		for(Failure failure : junitResult.getFailures()) {
			Autograder autoAnno = failure.getDescription().getAnnotation(Autograder.class);
			if(!isAnnotationPresent(failure.getDescription(), autoAnno)) {
				System.err.println("Annotation not present on " + failure.getTestHeader());
				continue;
			}
			String groupName = autoAnno.group();
			int missedPointsForTest = getMissedPointsForTest(failure, autoAnno);
			String errorMessage = "TEST FAILED: " + failure.getDescription().getMethodName();

			if(failure.getMessage() != null) {
				errorMessage += " - " + failure.getMessage();
			} if(failure.getException() instanceof NullPointerException) { // for some reason, JUnit doesn't capture NPEs
				errorMessage += " - NullPointerException. " + failure.getException().getStackTrace()[0].toString();
			}
			
			autoResult.addFailure(groupName, errorMessage, missedPointsForTest);
		}
		return autoResult;
	}

	private Map<String, Integer> getGroups(Class<?> junitGradingClass) {
		Map<String, Integer> groupToPoints = new HashMap<>();
		Groups groups = junitGradingClass.getAnnotation(Groups.class);
		for(Group group : groups.value()) {
			groupToPoints.put(group.name(), group.totalPoints());
		}
		if(groups.value().length == 0) {
			throw new IllegalStateException("The JUnit test case being run by the autograder doesn't know its total points. Use the Group annotation");
		}
		return groupToPoints;
	}

	private boolean isAnnotationPresent(Description description, Annotation anno) {
		if(description != null) {
			return !(description.isTest() && anno == null);
		}
		return true;
	}
	
	/* We have to keep track of missed points, because the JUnit runner only tracks failed tests. We cannot sum up 
	 * points we don't know about! We can only subract missed points from a known total. 
	 **/
	private int getMissedPointsForTest(Failure failure, Autograder autoAnno) {
		if(failure.getException() instanceof AutograderTestException) {
			AutograderTestException autoException = (AutograderTestException)failure.getException();
			return autoAnno.pointsPossible() - autoException.getPointsEarned();
		}
		return autoAnno.pointsPossible() - autoAnno.pointsOnFailure();
	}
}
