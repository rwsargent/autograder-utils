package autograderutils.results;

import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JUnitAutograderResultFromFile extends AutograderResult{

	private int score, totalPoints;
	private String feedback;
	private String summary;
	private String testResults;
	
	public JUnitAutograderResultFromFile(InputStream resultFile) {
		try(Scanner scanner = new Scanner(resultFile)) {
			StringBuilder stringBuilder = new StringBuilder();
			String line = null;
			int testResultStart = 0;
			while(scanner.hasNextLine()) {
				line = scanner.nextLine();
				if(line.startsWith("----")) {
					feedback = stringBuilder.toString();
					testResultStart = stringBuilder.length() + line.length() + 1;
				}
				
				stringBuilder.append(line)
					.append('\n'); // scanner strips line ending, add it back in.
			}
			summary = stringBuilder.toString();
			testResults = stringBuilder.substring(testResultStart);
		}
		
		// Grab numbers from feedback;
		try {
			if(feedback != null) {
				Matcher matcher = Pattern.compile("\\d+").matcher(feedback);
				matcher.find();
				score = Integer.parseInt(matcher.group());
				
				matcher.find();
				totalPoints = Integer.parseInt(matcher.group());
			}
		} catch(Exception e) {
			// Swallow this exception so that the object is constructed.
		}
	}

	@Override
	public int getScore() {
		return score;
	}

	@Override
	public int getTotal() {
		return totalPoints;
	}

	@Override
	public String getFeedback() {
		return feedback;
	}

	@Override
	public String getSummary() {
		return summary;
	}

	@Override
	public String getTestResults() {
		return testResults;
	}

	@Override
	public int getNumberOfTests() {
		return 0;
	}
	
}
