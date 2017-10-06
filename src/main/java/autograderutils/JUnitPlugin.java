package autograderutils;


import java.lang.annotation.Annotation;
import java.util.Arrays;
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
import autograderutils.results.JUnitAutograderResult;

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
	public JUnitAutograderResult grade(Class<?> junitGradingClass) {
		// Grab the toal number of points from 
		Map<String, Integer> totalGroupPoints = getGroups(junitGradingClass);
		
		JUnitCore core = new JUnitCore();
		core.addListener(new RunListener());
		Result junitResult = core.run(junitGradingClass);
		JUnitAutograderResult autoResult = new JUnitAutograderResult(totalGroupPoints, junitResult);
		
		for(Failure failure : junitResult.getFailures()) { 
			Autograder autoAnno = failure.getDescription().getAnnotation(Autograder.class);
			if(!isAnnotationPresent(failure.getDescription(), autoAnno)) {
				System.err.println("Annotation not present on " + failure.getTestHeader());
				if(failure.getTestHeader().contains("initializationError")) {
					// JUnit couldn't run.
					autoResult.failed("Could not run JUnit file. " + failure.getMessage());
				}
				System.err.println(failure.getMessage());
				System.err.println(failure.getTrace());
				System.err.println(failure);
				continue;
			}
			String groupName = autoAnno.group();
			String errorMessage = "TEST FAILED: " + failure.getDescription().getMethodName();

			if(failure.getException() != null) {
				errorMessage += " - " + failure.getException().getClass().getSimpleName();
			}
			
			if(failure.getMessage() != null) {
				errorMessage += " - " + failure.getMessage();
			} else {
				errorMessage += "@ " + maybeGetFirstLineOfStackTrace(failure);
			}
			
			autoResult.addTestFailure(groupName, errorMessage, getMissedPointsForTest(failure, autoAnno));
		}
		return autoResult;
	}

	private String maybeGetFirstLineOfStackTrace(Failure failure) {
		StackTraceElement[] stackTrace = failure.getException().getStackTrace();
		if(stackTrace != null && stackTrace.length > 0) {
			return stackTrace[0].toString();
		}
		return "";
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
	
	
	public static void main(String[] args) {
		if(args.length != 1) {
			System.err.println("Incorrect arguments. Supplied: " + Arrays.toString(args) + ", but expected the qualifed name for the grader suite class.");
			System.exit(-1);
			return;
		}
		
		Class<?> graderClass = null;
		try {
			graderClass = Class.forName(args[0]);
		} catch (ClassNotFoundException e) {
			System.err.println("Could not find the grader class (" + args[0] + ") needed!");
			System.exit(-1);
		}
		JUnitPlugin plugin = new JUnitPlugin();
		if(System.getProperty("java.security.policy") != null) {
			System.setSecurityManager(new SecurityManager(){
				@Override
				public void checkExit(int status) {
					throw new RuntimeException("DO NOT CALL System.exit() EVER. IN YOUR LIFE. JUST DON'T DO IT");
				}
			});
		}
		JUnitAutograderResult result = plugin.grade(graderClass);
		System.setSecurityManager(null);
		System.out.println(result.getSummary());
		
		System.exit(0); // yes I realize the irony. JUnit spins infinite threads, I have to manually kill the JVM. 
	}
}
