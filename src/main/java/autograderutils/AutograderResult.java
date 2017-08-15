package autograderutils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Wraps the JUnit result
 * @author ryans
 */
public class AutograderResult {

	private Map<String, Integer> totalPointsPerGroup;
	private Map<String, Integer> missedPoints;
	private HashMap<String, List<String>> errorMessages;
	
	int totalPoints = 0;

	public AutograderResult(Map<String, Integer> totalPoinsPossiblePerGroup) {
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
	}
	
	public void addFailure(String group, String message, int pointsEarned) {
		missedPoints.put(group, missedPoints.get(group) + pointsEarned);
	}
	
	public void printSummary(PrintStream out) {
		// print total first
		int score = calculateScore();
		out.println("Your submission received " + score + " out of " + totalPoints + " points possible.\n" + 
                    "Your canvas score will reflect this percentage.");
		out.println();
		
		//Helpful hints incoming
		out.println("<helpful hints here>");
		out.println("-----");
		
		// per group
		for(String group : totalPointsPerGroup.keySet()) {
			int totalPoints = totalPointsPerGroup.get(group);
			Integer missed = missedPoints.get(group);
			out.println(group + ":\t" + (totalPoints - missed) + " / " + totalPoints);
			errorMessages.get(group).forEach(out::println);
		}
	}

	private int calculateScore() {
		int missedTotal = 0;
		for(Entry<String, Integer> missedPoint: missedPoints.entrySet()) {
			missedTotal += missedPoint.getValue();
		}
		return totalPoints - missedTotal;
	}
}
