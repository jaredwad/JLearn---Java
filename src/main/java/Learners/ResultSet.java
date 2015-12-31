package Learners;

/**
 * Created by jared on 12/3/15.
 */
public class ResultSet {
	private double accuracy;
	private double errorRate;
	private double precision;
	private double recall;

	public ResultSet(double pAccuracy, double pErrorRate, double pPrecision, double pRecall) {
		accuracy  = pAccuracy;
		errorRate = pErrorRate;
		precision = pPrecision;
		recall    = pRecall;
	}

	public double getAccuracy () { return accuracy; }
	public double getErrorRate() { return errorRate; }
	public double getPrecision() { return precision; }
	public double getRecall   () { return recall   ; }

	public void setAccuracy (double pAccuracy ) { accuracy  = pAccuracy ; }
	public void setErrorRate(double pErrorRate) { errorRate = pErrorRate; }
	public void setPrecision(double pPrecision) { precision = pPrecision; }
	public void setRecall   (double pRecall   ) { recall    = pRecall   ; }
}
