package autograderutils.results;

public abstract class AutograderResult {
	
	public abstract int getScore();
	
	public abstract int getTotal();
	
	public abstract String getFeedback();
	
	public abstract String getSummary();
	
	public abstract String getTestResults();
	
}
