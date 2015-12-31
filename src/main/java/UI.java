import DataAccess.*;
import InstanceSelecters.AInstanceSelecter;
import InstanceSelecters.MarginInstanceSelecter;
import InstanceSelecters.RandomInstanceSelecter;
import Learners.PoolLearner;
import Oracles.IOracle;
import Oracles.MetaDataOracle;
import Tests.JlearnTest;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Instance;

import java.util.*;

/**
 * Created by jared on 12/29/15.
 */
public class UI {

	public void run() {
		String[] options = {
				"JLearn Test"
				, "Custom"
		};

		switch(getOption(options)){
			case 1:
				new JlearnTest().run();
				break;
			default:
				IDataAccessObject dao = buildDAO();
				IOracle oracle = buildOracle(dao);
				AInstanceSelecter selecter = buildSelecter();
				PoolLearner learner = new PoolLearner(oracle, selecter);

				JlearnDataAccessObject jdao = new JlearnDataAccessObject();
				List<String> ids = jdao.getIds();
				HashMap<String, Instance> data = new HashMap<String, Instance>();

				for (String id : ids) {
					Instance i = jdao.getInstance(id);

					data.put(id, i);
				}

				HashMap<String, Instance> testData = createTestData(data, oracle, .2);


				long startTime = System.nanoTime();
				Classifier classifier = learner.learn(data, new HashMap<String, Instance>(), testData, .8, 5);
				long endTime = System.nanoTime();

				double duration = ((double) endTime - (double) startTime)/1000000000d; //Get durration in seconds
				double accuracy = learner.validate();
				double numRuns  = learner.getNumRuns();

				System.out.println("Ran " + numRuns + " tests");
				System.out.println("Finished with accuracy: " + accuracy);
				System.out.println("It took " + duration + "seconds");
		}
	}

	private IDataAccessObject buildDAO() {
		String[] options = {
				"JLearn Dao"
				, "MetaData Dao"
		};

		switch (getOption(options)){
			case 1:
				return new JlearnDataAccessObject();
			default:
				return new MysqlMetaDataAccessObject();
		}

	}

	private IOracle buildOracle(IDataAccessObject dao) {
		String[] options = {
				"Meta Data Oracle"
		};

		switch (getOption(options)) {
			default:
				return new MetaDataOracle(dao, null, null);
		}
	}

	private AInstanceSelecter buildSelecter() {
		String[] options = {
				"Random"
				, "margin"
		};

		switch (getOption(options)) {
			case 1:
				return new RandomInstanceSelecter();
			default:
				return new MarginInstanceSelecter();
		}
	}

	private void printOptions(String[] options) {
		int i = 1;
		for(String s : options) {
			System.out.println(i + ": " + s);
			i++;
		}
	}

	private int getOption(String[] options) {
		int max = options.length;
		int selection = 0;

		printOptions(options);

		while(true) {
			System.out.println("Please enter a number between 1 and " + max);
			String s = null;
			Scanner in = new Scanner(System.in);

			try{
//				selection = Integer.parseInt(s);
				selection = in.nextInt();
			} catch(NumberFormatException e) {
				continue;
			} catch(InputMismatchException e) {
				continue;
			}

			if(selection > 0 && selection <= max)
				break;
		}
		return selection;
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
		new UI().run();
	}
}
