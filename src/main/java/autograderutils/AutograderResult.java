package autograderutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * Wraps the JUnit result
 * @author ryans
 */
public class AutograderResult {

	private Map<String, Integer> totalPointsPerGroup;
	private Map<String, Integer> missedPoints;
	private HashMap<String, List<String>> errorMessages;
	private Result junitResult;
	
	private List<String> failureMessages;
	
	int totalPoints = 0;

	public AutograderResult(Map<String, Integer> totalPoinsPossiblePerGroup, Result junitResult) {
		this.junitResult = junitResult;
		
		this.totalPointsPerGroup = totalPoinsPossiblePerGroup;
		for(int points : this.totalPointsPerGroup.values()) {
			totalPoints += points; 
		}
		missedPoints = new HashMap<>(this.totalPointsPerGroup);
		errorMessages = new HashMap<>();
		for(Entry<String, Integer> entry : missedPoints.entrySet()) {
			entry.setValue(0);
			
			errorMessages.put(entry.getKey(), new ArrayList<>());
		}
		
		failureMessages = new ArrayList<>();
	}
	
	public void addTestFailure(String group, String message, int pointsEarned) {
		if(missedPoints.get(group) == null) {
			throw new RuntimeException("The group " + group + " is not annotated on the Grader");
		}
		missedPoints.put(group, missedPoints.get(group) + pointsEarned);
		errorMessages.get(group).add(message);
	}
	
	public String getFeedback() {
		StringBuilder out = new StringBuilder();
		out.append(getScoreLine());
		maybeAddHelpfulHints(out);
		return out.toString();
	}
	
	public String getScoreLine() {
		return "Your submission received " + calculateScore() + " out of " + totalPoints + " points possible.\n" + 
                "Your canvas score will reflect this percentage.\n";
	}
	
	public String buildSummary() {
		// print total first
		StringBuilder out = new StringBuilder();
		out.append(getScoreLine());
		
		maybeAddHelpfulHints(out);
		out.append("-----\n");
		
		// per group
		out.append(buildTestResults());
		return out.toString();
	}

	public String buildTestResults() {
		StringBuilder out = new StringBuilder();
		for(String group : totalPointsPerGroup.keySet()) {
			int totalPoints = totalPointsPerGroup.get(group);
			Integer missed = missedPoints.get(group);
			out.append(group + ":\t" + (totalPoints - missed) + " / " + totalPoints + "\n");
			errorMessages.get(group).forEach(msg -> out.append(msg + "\n"));
			out.append("\n");
		}
		return out.toString();
	}

	private void maybeAddHelpfulHints(StringBuilder out) {
		StringBuilder localbuilder = new StringBuilder();
		for(Failure failure : junitResult.getFailures()) {
			if(exceptCauseIs(failure.getException(), ClassCastException.class)) {
				localbuilder.append(failure.getTestHeader() + ":" + failure.getMessage() + "\n");
			}	
			if(failure.getMessage() != null && failure.getMessage().contains("Unresolved compilation")) {
				localbuilder.append(failure.getTestHeader() + ":" + failure.getMessage() + "\n");
			}
		}
		
		if(localbuilder.length() > 0 ) {
			out.append("\nHere are some of the tests that failed, and why\n");
			out.append(localbuilder.toString());
		}
	}

	private boolean exceptCauseIs(Throwable exception, Class<? extends Throwable> causeException) {
		if(causeException.isInstance(exception)) {
			return true;
		}
		if(exception.getCause() == null) {
			return false;
		}
		return exceptCauseIs(exception.getCause(), causeException); 
	}

	private int calculateScore() {
		if(junitResult.getRunCount() <= 1) {
			return 0;
		}
		int missedTotal = 0;
		for(Entry<String, Integer> missedPoint: missedPoints.entrySet()) {
			missedTotal += missedPoint.getValue();
		}
		return totalPoints - missedTotal;
	}
	
	public int getScore() {
		return calculateScore();
	}
	
	public int getTotal() {
		return totalPoints;
	}
}
