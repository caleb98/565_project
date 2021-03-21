

public class NoTokenException extends Exception {

	private static final long serialVersionUID = 5250689285932170086L;
	
	public NoTokenException() {
		super("No token available for request.");
	}

}
