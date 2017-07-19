package autograderutils;

public class AutograderTestException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int pointsEarned = 0;
	
	public AutograderTestException() {
		super("Not implemented");
	}
	
	public AutograderTestException(int pointsEarned, String message) {
		super(message);
		this.pointsEarned = pointsEarned;
	}
	
	public int getPointsEarned() {
		return pointsEarned;
	}
}
