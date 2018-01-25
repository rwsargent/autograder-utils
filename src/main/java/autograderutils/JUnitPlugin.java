package autograderutils;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.util.Arrays;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import autograderutils.annotations.Autograder;
import autograderutils.results.JUnitAutograderResult;

/**
 * Used to run a JUnit test suite and pipe its results to stdout. 
 * @author ryans
 *
 */
public class JUnitPlugin {
	public JUnitAutograderResult grade(Class<?> junitGradingClass) {
		Result junitResult;
		Metadata metadata = Metadata.from(junitGradingClass);
		try {
			JUnitCore core = new JUnitCore();
			core.addListener(new RunListener());
			junitResult = core.run(junitGradingClass);
		} catch(Throwable t) {
			junitResult = new Result();
			JUnitAutograderResult failResult = new JUnitAutograderResult(metadata, junitResult);
			failResult.addRunFailure(t.getMessage());
			return failResult;
		}
		JUnitAutograderResult autoResult = new JUnitAutograderResult(metadata, junitResult);
		
		for(Failure failure : junitResult.getFailures()) { 
			Autograder autoAnno = failure.getDescription().getAnnotation(Autograder.class);
			if(!isAnnotationPresent(failure.getDescription(), autoAnno)) {
				System.err.println("Annotation not present on " + failure.getTestHeader());
				if(failure.getTestHeader().contains("initializationError")) {
					// JUnit couldn't run.
					autoResult.failed("Could not run JUnit file. " + failure.getTrace());
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
				errorMessage += " @ " + maybeGetFirstLineOfStackTrace(failure);
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
		
		PrintStream stdOut = System.out;
		System.setOut(new PrintStream(new DevNull()));
		
		if(System.getProperty("java.security.policy") != null) {
			System.setSecurityManager(new SecurityManager(){
				@Override
				public void checkExit(int status) {
					throw new RuntimeException("DO NOT CALL System.exit() EVER. IN YOUR LIFE. JUST DON'T DO IT");
				}
			});
		}
		
		JUnitPlugin plugin = new JUnitPlugin();
		JUnitAutograderResult result = plugin.grade(graderClass);
		System.setSecurityManager(null);
		System.setOut(stdOut);
		stdOut.println(result.getSummary());
		
		System.exit(0); // yes I realize the irony. JUnit spins infinite threads, I have to manually kill the JVM. 
	}
	
	private static class DevNull extends OutputStream {
		@Override
		public void write(int b) throws IOException {}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {}
		
		@Override
		public void write(byte[] b) throws IOException {}
	}
}
