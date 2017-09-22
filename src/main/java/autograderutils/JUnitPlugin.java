package autograderutils;


import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

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
 * If no groups are neccessary, the second argument needs to be the number of total possible pints in the grader script. 
 * The @Autograder annotation will default to "Autograder," but the extra.properites file is always necessary.
 *
 * @author ryans
 *
 */
public class JUnitPlugin {
	public AutograderResult grade(Class<?> junitGradingClass) {
		// Grab the toal number of points from 
		Map<String, Integer> totalGroupPoints = getGroups(junitGradingClass);
		
		JUnitCore core = new JUnitCore();
		core.addListener(new RunListener());
		Result junitResult = core.run(junitGradingClass);
		AutograderResult autoResult = new AutograderResult(totalGroupPoints, junitResult);
		
		for(Failure failure : junitResult.getFailures()) {
			Autograder autoAnno = failure.getDescription().getAnnotation(Autograder.class);
			if(!isAnnotationPresent(failure.getDescription(), autoAnno)) {
				
				System.err.println("Annotation not present on " + failure.getTestHeader());
				continue;
			}
			String groupName = autoAnno.group();
			String errorMessage = "TEST FAILED: " + failure.getDescription().getMethodName();

			if(failure.getException() instanceof ArrayIndexOutOfBoundsException) {
				errorMessage += " - ArrayIndexOutOfBoundException ";
			} else if(failure.getException() instanceof NullPointerException) { // for some reason, JUnit doesn't capture NPEs
				errorMessage += " - NullPointerException. ";
				errorMessage = maybeAddFirstLineOfStackTrace(failure, errorMessage);
			} else if (failure.getException() instanceof IndexOutOfBoundsException) {
				errorMessage += " - IndexOutOfBoundsException " + failure.getException().getStackTrace()[0].toString();
				errorMessage = maybeAddFirstLineOfStackTrace(failure, errorMessage);
			}
			
			if(failure.getMessage() != null) {
				errorMessage += " - " + failure.getMessage();
			}
			
			autoResult.addTestFailure(groupName, errorMessage, getMissedPointsForTest(failure, autoAnno));
		}
		return autoResult;
	}

	private String maybeAddFirstLineOfStackTrace(Failure failure, String errorMessage) {
		StackTraceElement[] stackTrace = failure.getException().getStackTrace();
		if(stackTrace != null && stackTrace.length > 0) {
			errorMessage += stackTrace[0].toString();
		}
		return errorMessage;
	}

	private Map<String, Integer> getGroups(Class<?> junitGradingClass) {
		Map<String, Integer> groupToPoints = new HashMap<>();
		Group[] groups = getGroupAnnotations(junitGradingClass);
		
		for(Group group : groups) {
			groupToPoints.put(group.name(), group.totalPoints());
		}
		
		return groupToPoints;
	}

	private Group[] getGroupAnnotations(Class<?> junitGradingClass) {
		Group[] groups = null; 
		Groups groupsAnno = junitGradingClass.getAnnotation(Groups.class);
		if(groupsAnno == null) {
			Group group = junitGradingClass.getAnnotation(Group.class);
			if(group == null) {
				throw new IllegalStateException("There are no Autograder group annotations on " + junitGradingClass.getName());
			}
			groups =  new Group[]{group};
		} else {
			groups = groupsAnno.value();
		}
		return groups;
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
