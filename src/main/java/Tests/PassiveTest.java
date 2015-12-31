package Tests;

import DataAccess.ArffDataAccessObject;
import DataAccess.IDataAccessObject;
import DataAccess.MysqlMetaDataAccessObject;
import InstanceSelecters.AInstanceSelecter;
import InstanceSelecters.MarginInstanceSelecter;
import InstanceSelecters.RandomInstanceSelecter;
import Learners.PoolLearner;
import Oracles.IOracle;
import Oracles.MetaDataOracle;
import Utilities.ClassifierUtils;
import Utilities.OpenMLUtils;
import Utilities.StringUtils;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Instance;

import java.util.*;

/**
 * Created by jared on 12/9/15.
 */
public class PassiveTest implements ITester {

	PoolLearner learner;
	HashMap<String, Instance> data;

	public PassiveTest() {
		String url = "jdbc:mysql://localhost:8889/";
		String dbName = "openML";
		String driver = "com.mysql.jdbc.Driver";
		String userName = "root";
		String password = "cocobear1";

		List<String> algorithms = createSimpleClassifierList();

		HashMap<String, Classifier> classifiers = ClassifierUtils.createClassifiers(algorithms);

		List<String> algorithmNames = createSimpleAlgorithmNameList();

		IDataAccessObject dao = new MysqlMetaDataAccessObject(url, dbName, driver, userName, password, algorithmNames);

//		int[] ids = createIds(100, 1, 1, dao);
//		int[] ids = createSimpleIds();
		int[] ids = OpenMLUtils.getIds(100000);

		HashMap<String, IDataAccessObject> datasets = new HashMap<String, IDataAccessObject>();
		data = new HashMap<String, Instance>();

		System.out.println("Getting Data Access Objects for each id");
		for(int id : ids) {
			String s = String.valueOf(id);

			if(StringUtils.isNullOrWhiteSpace(s))
				continue;

			ArffDataAccessObject arffDAO = OpenMLUtils.getArffDAO(s);

			if(arffDAO != null) {
				data.put(s, dao.getInstance(s));
				datasets.put(s, arffDAO);
			}
		}

		IOracle oracle = new MetaDataOracle(dao, classifiers, datasets);

		AInstanceSelecter instanceSelecter = new RandomInstanceSelecter();

		learner = new PoolLearner(oracle, instanceSelecter);
	}


	public void run() {
		learner.learn(data,new HashMap<String, Instance>(), .9, 10);
	}

	private List<String> createSimpleClassifierList() {
		String[] classifierList = {
				  "weka.classifiers.rules.ZeroR"
				, "weka.classifiers.rules.OneR -- -B 6"
				, "weka.classifiers.bayes.NaiveBayes"
				, "weka.classifiers.trees.J48 -- -C 0.25 -M 2"
				, "weka.classifiers.trees.DecisionStump"
				, "weka.classifiers.functions.Logistic -- -R 1.0E-8 -M -1"
				, "weka.classifiers.lazy.KStar -- -B 20 -M a"
				, "weka.classifiers.trees.RandomForest -- -I 101 -K 0 -S 1 -num-slots 1"
				, "weka.classifiers.functions.MultilayerPerceptron -- -L 0.5 -M 0.2 -N 500 -V 0 -S 0 -E 20 -H a"
		};
		return Arrays.asList(classifierList);
	}

	private List<String> createSimpleAlgorithmNameList() {
		String[] classifierList = {
				"weka.ZeroR(%)"
				, "weka.OneR(%)"
				, "weka.NaiveBayes(%)"
				, "weka.J48(%)"
				, "weka.DecisionStump(%)"
				, "weka.Logistic(%)"
				, "weka.KStar(%)"
				, "weka.RandomForest(%)"
				, "weka.MultilayerPerceptron(%)"
		};
		return Arrays.asList(classifierList);
	}

	public static int[] createSimpleIds() {
		System.out.println("Creating simple ids");

		int max = 200;
//		int max = 10;

		int[] ids = new int[max];

		int index = 0;

		for(; index < 62 && index < max; ++index) {
			ids[index] = index + 1;
		}

		for(int i = 163; i < 232 && index < max; ++i) {
			ids[index] = i;
			index++;
		}

//		ids[index] = 265;
//		index++;

		for(int i = 273; i < 280 && index < max; ++i) {
			ids[index] = i;
			index++;
		}

//		ids[index] = 285;
//		index++;

//		ids[index] = 287;
//		index++;

		for(int i = 273; i < 280 && index < max; ++i) {
			ids[index] = i;
			index++;
		}

		return ids;
	}

	public static int[] createIds(int size, int start, int offset, IDataAccessObject dao) {
		System.out.println("Creating Test ids");

		if(size < 1)
			size = 1;
		if(size > 500)
			size = 500;
		if(start < 1 || start > 500)
			start = 1;
		if(offset < 1 || offset * size > 500)
			offset = 1;

		int[] ids = new int[size];

		for(int i = 0; i < size; ++i) {
			ids[i] = start;

			String label = (String) dao.getLabel(String.valueOf(start));
			if(StringUtils.isNullOrWhiteSpace(label)) {
				i--;
			}

			start += offset;
		}
		System.out.println("#####Successfully created Test ids");
		return ids;
	}
}
