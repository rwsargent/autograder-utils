package autograderutils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.runners.Suite.SuiteClasses;

import autograderutils.annotations.Autograder;

/**
 * 
 * @author ryans
 */
public class Metadata {
	public Map<String, Integer> totalGroupPoints = new HashMap<>();
	public int numberOfTests;
	
	
	public static Metadata from(Class<?> junitGradingClass) {
		Metadata md = new Metadata();
		// Might be a suite
		SuiteClasses suiteClasses = junitGradingClass.getAnnotation(SuiteClasses.class);
		if(suiteClasses != null) {
			for(Class<?> testClass : suiteClasses.value()) {
				fillMetadataFromClass(testClass, md);
			}
		} else {
			fillMetadataFromClass(junitGradingClass, md);
		}
		return md;
	}
	
	private static void fillMetadataFromClass(Class<?> junitGradingClass, Metadata md) {
		for (Method method : junitGradingClass.getDeclaredMethods()) {
			Autograder autoAnno = method.getAnnotation(Autograder.class);
			if(autoAnno != null) {
				md.numberOfTests++;
				md.totalGroupPoints.compute(autoAnno.group(), (k, v) -> v == null
						? autoAnno.pointsPossible() 
						: v + autoAnno.pointsPossible());
			}
		}
	}
}
