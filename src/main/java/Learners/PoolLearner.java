package Learners;

import InstanceSelecters.AInstanceSelecter;
import InstanceSelecters.MarginInstanceSelecter;
import InstanceSelecters.RandomInstanceSelecter;
import Oracles.IOracle;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.KNearestNeighbors;
import net.sf.javaml.classification.evaluation.CrossValidation;
import net.sf.javaml.classification.evaluation.EvaluateDataset;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.sampling.Sampling;

import java.util.*;

/**
 * Created by jared on 12/3/15.
 */
public class PoolLearner {
	private HashMap<String, Instance> unlabeledData;
	private HashMap<String, Instance> labeledData;
	private HashMap<String, Instance> testData;
	private IOracle                   oracle;
	private AInstanceSelecter         selecter;
	private Classifier                classifier;
	private ResultSet                 latestResults;

	private int numRuns;

	public PoolLearner(IOracle pOracle) {
		this(pOracle, new RandomInstanceSelecter(), new KNearestNeighbors(3));
	}
	public PoolLearner(IOracle pOracle , AInstanceSelecter pSelecter) {
		this(pOracle, pSelecter, new KNearestNeighbors(3));
	}
	public PoolLearner(IOracle pOracle , AInstanceSelecter pSelecter , Classifier pClassifier) {

		System.out.println("Initializing Pool learner");

		oracle = pOracle;
		selecter = pSelecter;
		classifier = pClassifier;

		numRuns = 0;

		selecter.setLearner(this);
	}

	public HashMap<String, Instance> getUnlabeledData() { return unlabeledData; }
	public HashMap<String, Instance> getLabeledData  () { return labeledData  ; }

	public Dataset getUnlabledAsDataset() { return mapToDataset(unlabeledData); }
	public Dataset getLabledAsDataset  () { return mapToDataset(labeledData  ); }

	public Instance getUnlabeledInstanceFromID(String id) { return unlabeledData.get(id); }
	public Instance getLabeledInstanceFromID  (String id) { return labeledData  .get(id); }
	public Instance getInstanceFromID         (String id) {
		if     (hasUnlabeledInstance(id)) { return getUnlabeledInstanceFromID(id); }
		else if(hasLabeledInstance  (id)) { return getLabeledInstanceFromID  (id); }
		return null;
	}

	public Classifier getClassifier() { return classifier; }

	public int getLabeledSize  () { return labeledData  .size(); }
	public int getUnlabeledSize() { return unlabeledData.size(); }

	public int getNumRuns() { return numRuns; }

	public boolean hasUnlabeledInstance(String id) { return unlabeledData.containsKey(id); }
	public boolean hasLabeledInstance  (String id) { return labeledData  .containsKey(id); }
	public boolean hasInstance         (String id) { return hasLabeledInstance(id) || hasUnlabeledInstance(id); }

	public Classifier learn(HashMap<String, Instance> unlabeledData) { return learn(unlabeledData
			, new HashMap<String,Instance>(), .7, (int) Math.ceil(unlabeledData.size() * .1)); }
	public Classifier learn(HashMap<String, Instance> unlabeledData, double desiredAccuracy) {
		return learn(unlabeledData, new HashMap<String,Instance>(), desiredAccuracy
				, (int) Math.ceil(unlabeledData.size() * .1));
	}
//	public Classifier learn(HashMap<String, Instance> pUnlabeledData
//			, HashMap<String, Instance> pLabeledData
//			, double desiredAccuracy) {
//		HashMap<String, Instance> testData = new HashMap<String, Instance>();
//
//		int size = (pLabeledData.size() + pUnlabeledData.size())
//				- (int) Math.ceil((pLabeledData.size() + pUnlabeledData.size()) * .8);
//
//		for(String key : pLabeledData.keySet()) {
//			testData.put(key, pLabeledData.remove(key));
//		}
//
//		Random rand = new Random();
//
//		while(size > testData.size()) {
//			int i = rand.nextInt(pUnlabeledData.size());
//
//			String nextID = "1";
//
//			for(String key : pUnlabeledData.keySet()) {
//				if(i <= 0) {
//					nextID = key;
//					break;
//				}
//				i--;
//			}
//
//			Instance instance = pUnlabeledData.remove(nextID);
//
//			instance.setClassValue(oracle.getLabel(nextID));
//
//			testData.put(nextID, instance);
//		}
//
//		return learn(pUnlabeledData, pLabeledData, testData, desiredAccuracy, 10);
//	}

	public Classifier learn(HashMap<String, Instance> pUnlabeledData
		, HashMap<String, Instance> pLabeledData
		, double desiredAccuracy
		, int numInitialRuns
	){
		HashMap<String, Instance> testData = createTestData(pUnlabeledData, pLabeledData, .2);

		return learn(pUnlabeledData, pLabeledData, testData, desiredAccuracy, numInitialRuns);
	}

	public Classifier learn(HashMap<String, Instance> pUnlabeledData
			, HashMap<String, Instance> pLabeledData
			, HashMap<String, Instance> pTestData
			, double desiredAccuracy
			, int numInitialRuns
	){
		unlabeledData = pUnlabeledData;
		labeledData = pLabeledData;
		testData = pTestData;

		double accuracy = 0;

//		while ((accuracy < desiredAccuracy || numInitialRuns > 0) && unlabeledData.size() > 0) {
		while ((accuracy < desiredAccuracy || numRuns < numInitialRuns) && unlabeledData.size() > 0) {
			String nextID = selecter.getNext();

			runOne(nextID);

			System.out.print("Run: " + numRuns);

			if(numRuns > numInitialRuns) {

				accuracy = validate();

				System.out.print(" Accuracy: " + accuracy);
			}

			System.out.println();
//			numInitialRuns = (numInitialRuns > 0 ? numInitialRuns - 1 : 0);
		}

		return classifier;
	}

	public double validate() {
		try {
			Map<Object, PerformanceMeasure> results = EvaluateDataset.testDataset(classifier, mapToDataset(testData));
			latestResults = mapToResultSet(results);
		} catch(NullPointerException e) {
//			e.printStackTrace();
			return 0;
		}

		return latestResults.getAccuracy();
	}

	private Dataset mapToDataset(HashMap<String, Instance> data) { return new DefaultDataset(data.values()); }

	private ResultSet mapToResultSet(Map<Object, PerformanceMeasure> results) {

		double i = 0;
		double accuracy  = 0;
		double errorRate = 0;
		double precision = 0;
		double recall    = 0;


		for(Object key : results.keySet()) {
			PerformanceMeasure performanceMeasure = results.get(key);

			accuracy  += performanceMeasure.getAccuracy();
			errorRate += performanceMeasure.getErrorRate();
			precision += performanceMeasure.getPrecision();
			recall    += performanceMeasure.getRecall();

			++i;
		}

		return new ResultSet( accuracy / i, errorRate / i, precision / i, recall / i );
	}

	private void runOne(String nextID) {
		Instance instance = unlabeledData.get(nextID);

		Object label = oracle.getLabel(nextID);

		if(label == null) {
			unlabeledData.remove(nextID);
		} else {

			instance.setClassValue(label);

			unlabeledData.remove(nextID);
			labeledData.put(nextID, instance);

			classifier.buildClassifier(mapToDataset(labeledData));

			numRuns++;
		}
//		Map<Object, PerformanceMeasure> results = EvaluateDataset.testDataset(classifier, mapToDataset(testData));
	}

	private String getOneRandomId() {
		Set<String> keySet = unlabeledData.keySet();

		int i = new Random(1).nextInt(keySet.size() - 1); //Todo remove the 1 from the random constructor so its truely random

		String id = "1";

		for(String key : keySet) {
			if(i <= 0) {
				id = key;
				break;
			}
			i--;
		}

		return id;
	}

	private HashMap<String, Instance> createTestData(HashMap<String, Instance> pUnlabeledData
			, HashMap<String, Instance> pLabeledData, double percentTest) {
		System.out.println("Creating test data");

		HashMap<String, Instance> testData = new HashMap<String, Instance>();

		double dataSize = pUnlabeledData.size() + pLabeledData.size();

		int numTest = (int) Math.ceil(percentTest * dataSize);

		System.out.println("\tAdding " + numTest + " Instances to the test data");

		Random generator = new Random();

		while(numTest > 0 && pLabeledData.size() > 0) {
			Object[] keys = pLabeledData.keySet().toArray();
			String nextID = (String) keys[generator.nextInt(keys.length)];

			testData.put(nextID, pLabeledData.remove(nextID));
			numTest--;
		}

		while(numTest > 0 && pUnlabeledData.size() > 0) {
			Object[] keys = pUnlabeledData.keySet().toArray();
			String nextID = (String) keys[generator.nextInt(keys.length)];
			Object label = oracle.getLabel(nextID);

			Instance instance = pUnlabeledData.remove(nextID);
			instance.setClassValue(label);

			testData.put(nextID, instance);

			numTest--;
		}

		System.out.println("Finished creating test data");
		return testData;
	}
}
