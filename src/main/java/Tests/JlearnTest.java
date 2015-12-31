package Tests;

import DataAccess.JlearnDataAccessObject;
import InstanceSelecters.AInstanceSelecter;
import InstanceSelecters.MarginInstanceSelecter;
import InstanceSelecters.RandomInstanceSelecter;
import Learners.PoolLearner;
import Oracles.IOracle;
import Oracles.MetaDataOracle;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by jared on 12/20/15.
 */
public class JlearnTest implements ITester {
	public void run() {
		JlearnDataAccessObject jdao = new JlearnDataAccessObject();

		MetaDataOracle oracle = new MetaDataOracle(jdao, null, null);

		int count    = 0;
		int numTests = 5;

		double desiredAccuracy = .83;
		int minNumTests = 5;

		double totalDuration = 0;
		double totalAccuracy = 0;
		double totalNumRuns  = 0;

		List<String> ids = jdao.getIds();
		HashMap<String, Instance> data = new HashMap<String, Instance>();

		for (String id : ids) {
			Instance i = jdao.getInstance(id);

			data.put(id, i);
		}

		HashMap<String, Instance> testData = createTestData(data, oracle, .2);

		// Create Passive Learner
		while (count < numTests) {
			AInstanceSelecter selecter = new RandomInstanceSelecter();

			PoolLearner passiveLearner = new PoolLearner(oracle, selecter);

			HashMap<String, Instance> clone = (HashMap<String, Instance>) data.clone();

			long startTime = System.nanoTime();
			Classifier classifier = passiveLearner.learn(clone, new HashMap<String, Instance>(), testData
					, desiredAccuracy, minNumTests);
			long endTime = System.nanoTime();

			double duration = ((double) endTime - (double) startTime)/1000000000d; //Get durration in seconds
			double accuracy = passiveLearner.validate();
			double numRuns  = passiveLearner.getNumRuns();

			System.out.println("Finished Building Passive Learner #" + count );
			System.out.println("Ran " + numRuns + " tests");
			System.out.println("Finished with accuracy: " + accuracy);
			System.out.println("It took " + duration + "seconds");

			totalDuration += duration;
			totalAccuracy += accuracy;
			totalNumRuns += numRuns;

			count++;
		}

		double averagePassiveAccuracy = totalAccuracy / numTests;
		double averagePassiveDuration = totalDuration / numTests;
		double averagePassiveNumRuns  = totalNumRuns  / numTests;


		totalAccuracy = 0;
		totalDuration = 0;
		totalNumRuns  = 0;
		count = 0;

		//Create Active Learner
		while (count < numTests) {
			AInstanceSelecter selecter = new MarginInstanceSelecter();

			PoolLearner activeLearner = new PoolLearner(oracle, selecter);

			HashMap<String, Instance> clone = (HashMap<String, Instance>) data.clone();

			long startTime = System.nanoTime();
			Classifier classifier = activeLearner.learn(clone, new HashMap<String, Instance>(), testData
					, desiredAccuracy - .04, minNumTests);
			long endTime = System.nanoTime();

			double duration = ((double) endTime - (double) startTime)/1000000000d; //Get durration in seconds
			double accuracy = activeLearner.validate();
			double numRuns  = activeLearner.getNumRuns();

			System.out.println("Finished Building Active Learner #" + count );
			System.out.println("Ran " + numRuns + " tests");
			System.out.println("Finished with accuracy: " + accuracy);
			System.out.println("It took " + duration + "seconds");

			totalDuration += duration;
			totalAccuracy += accuracy;
			totalNumRuns += numRuns;

			count++;
		}

		double averageActiveAccuracy = totalAccuracy / numTests;
		double averageActiveDuration = totalDuration / numTests;
		double averageActiveNumRuns  = totalNumRuns / numTests;

		System.out.println("Number of times learned " + numTests);

		System.out.println();
		System.out.println("****************************************************************");
		System.out.println("Passive Learner Stats:");
		System.out.println("Average Number of tests: " + averagePassiveNumRuns);
		System.out.println("Average accuracy: " + averagePassiveAccuracy);
		System.out.println("Average duration: " + averagePassiveDuration + " seconds");
		System.out.println("****************************************************************");
		System.out.println();

		System.out.println();
		System.out.println("****************************************************************");
		System.out.println("Active Learner Stats:");
		System.out.println("Average Number of tests: " + averageActiveNumRuns);
		System.out.println("Average accuracy: " + averageActiveAccuracy);
		System.out.println("Average duration: " + averageActiveDuration + " seconds");
		System.out.println("****************************************************************");
		System.out.println();

		System.out.println("The " + (averageActiveAccuracy >= averagePassiveAccuracy ? "Active" : "Passive")
								   + " Learner was more accurate");

		System.out.println("The " + (averageActiveDuration <= averagePassiveDuration ? "Active" : "Passive")
								   + " Learner was faster");

	}

	private HashMap<String, Instance> createTestData(HashMap<String, Instance> pUnlabeledData
			, IOracle oracle, double percentTest) {
		System.out.println("Creating test data JLearnTest");

		HashMap<String, Instance> testData = new HashMap<String, Instance>();

		double dataSize = pUnlabeledData.size();

		int numTest = (int) Math.ceil(percentTest * dataSize);

		System.out.println("\tAdding " + numTest + " Instances to the test data");

		Random generator = new Random();

		while (numTest > 0 && pUnlabeledData.size() > 0) {
			Object[] keys   = pUnlabeledData.keySet().toArray();
			String   nextID = (String) keys[generator.nextInt(keys.length)];
			Object   label  = oracle.getLabel(nextID);

			Instance instance = pUnlabeledData.remove(nextID);
			instance.setClassValue(label);

			testData.put(nextID, instance);

			numTest--;
		}

		System.out.println("Finished creating test data");
		return testData;
	}

	public static void main(String[] args) {
		new JlearnTest().run();
	}
}
