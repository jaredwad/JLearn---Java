package Log;

/**
 * Created by jared on 12/10/15.
 */
public class ConsoleLogger implements ILogger{
	boolean verbose;

	ConsoleLogger(boolean pVerbose) {
		verbose = pVerbose;
	}

	public void log(String message) {
		System.out.println(message);
	}

	public void logVerbose(String message) {
		if(verbose){
			log(message);
		}
	}
}
