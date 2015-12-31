package DataAccess;

import Utilities.ClassifierUtils;
import Utilities.OpenMLUtils;
import be.abeel.util.Pair;
import javassist.NotFoundException;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.evaluation.CrossValidation;
import net.sf.javaml.classification.evaluation.EvaluateDataset;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.*;
import net.sf.javaml.sampling.Sampling;

import java.sql.*;
import java.util.*;

/**
 * Created by jared on 12/19/15.
 */
public class JlearnDataAccessObject implements IDataAccessObject  {
	String url = "jdbc:mysql://localhost:8889/";
	String dbName = "jlearn";
	String driver = "com.mysql.jdbc.Driver";
	String userName = "root";
	String password = "cocobear1";
	Connection conn;

	List<String> algorithms;
	List<String> qualities;

	HashMap<String, Classifier> classifiers;

	public JlearnDataAccessObject() {
		System.out.println("Creating JlearnDataAccessObject");

		connect();

		algorithms = getAlgorithms();
		qualities = getQualities();

		classifiers = new HashMap<String, Classifier>();
	}

	public Instance getInstance(String id) {
		SparseInstance instance = new SparseInstance();

		Statement st = null;
		try {
			st = conn.createStatement();

			String query;
			ResultSet res;

			int i = 0;
			for(String quality : qualities) {
				try {
					query = "select * from data_attributes" +
							" where PK_ID=" + id +
							" and attribute='" + quality + "'";

					res = st.executeQuery(query);
					if(res.next()) {
						instance.put(i, res.getDouble("value"));
						if(res.wasNull())
							instance.put(i,0d);

					}
					else
						instance.put(i,0d);

				} catch (SQLException e) {
					e.printStackTrace();
				}
				++i;
			}
		} catch (SQLException e) {
			System.out.println("ERROR retrieving instance: " + id);
			e.printStackTrace();
		}
		return instance;
	}

	public Object getLabel(String id) {
//		System.out.println("Getting Label for id: " + id);

		String best = "";
		double highest = 0;

		for(String algorithm : algorithms) {
			try {
				double newValue = getValueForAlgorithm(id, algorithm);

				if(newValue < 0)
					return null;

				if(newValue > highest) {
					best = algorithm;
					highest = newValue;
				}

			} catch (NotFoundException e) {
				System.out.println("Couldn't find result for algorithm: " + algorithm);

//				removeFromDB(id);

				return null;
			}
		}

//		System.out.println("The label is: " + best);
		return best;
	}

	private void removeFromDB(String id) {
		System.out.println("Removing " + id + " from DB");

		String query = "delete from data where PK_ID = " + id;

		Statement st;

		try {
			st = conn.createStatement();
			st.execute(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean hasLabel(String id) { return getLabel(id) != null; }

	public Dataset getDataset() {
		List<String> ids = getIds();

		return getDataset(ids.toArray(new String[ids.size()]));
	}

	public Dataset getDataset(String[] ids) {
		System.out.println("Getting dataset from JLearn MySQL");
		Dataset data = new DefaultDataset();

		for(String id : ids) {
			Instance i = getInstance(id);

			if(id != null)
				data.add(i);
		}

		System.out.println("Finished Retrieving the Data!");
		return data;
	}

	public void saveInstance(String id, Instance instance) {}

	public List<String> getIds() {
		List<String> ids = new ArrayList<String>();

		Statement st;

		try {
			st = conn.createStatement();

			ResultSet resultSet = st.executeQuery("select PK_ID from data");

			while (resultSet.next()) {
				ids.add(String.valueOf(resultSet.getInt("PK_ID")));
			}


		} catch (SQLException e) {
			e.printStackTrace();
		}

		return ids;
	}

	private double getValueForAlgorithm(String id, String algorithm) throws NotFoundException {
		String query =
			"select d.PK_ID, d.name, a.name, r.accuracy from result r"
				+ " JOIN data d on d.PK_ID = r.FK_DATA_ID"
				+ " JOIN algorithm a on a.PK_ID = r.FK_ALGORITHM_ID"
				+ " where d.PK_ID=" + id
				+ " and a.name='" + algorithm + "'"
				+ " order by d.pk_id";

		Statement st;

		try {
			st = conn.createStatement();
			ResultSet res = st.executeQuery(query);

			res.next();

			double val = res.getDouble("accuracy");

			if(val != Double.NaN)
				return val;

		} catch (SQLException e) {
//			System.out.println("ERROR retrieving value for " + algorithm + " with id " + id);
		}

		try {
			st = conn.createStatement();
			ResultSet res = st.executeQuery("select setup_string from algorithm where name = '" + algorithm + "'");

			res.next();

			String setup = res.getString("setup_string");

			Classifier classifier = ClassifierUtils.createClassifier(setup);

			double result = runClassifier(id, classifier);

			if(result >= 0) {
				saveRun(id, algorithm, result);

				return result;
			}
		} catch (SQLException e) {
		} catch (Exception e) {
		}

		throw new NotFoundException("The result for " + algorithm + " on data set " + id + " could not be found.");
	}

	private List<String> getQualities() {
//		System.out.println("Getting possible data quality values from MySQL");

		List<String> qualities = new ArrayList<String>(100);
		Statement st;
		try {
			st = conn.createStatement();
			ResultSet res = st.executeQuery("select distinct attribute from data_Attributes");

			while (res.next()) {
				String name = res.getString("attribute");
				qualities.add(name);
			}

		} catch (SQLException e) {
			System.out.println("ERROR retrieving data qualities");
		}
		return qualities;
	}

	private double runClassifier(String id, Classifier classifier) {
		String openMLId;
		try {
			openMLId = getOpenmlId(id);
		} catch (NotFoundException e) {
			return -1;
		}

		ArffDataAccessObject arffDAO = OpenMLUtils.getArffDAO(openMLId);

		Dataset data = arffDAO.getDataset();

		if(data.size() == 0)
			return -1;

		Pair<Dataset, Dataset> data2 = Sampling.SubSampling.sample(data, (int) (data.size() * 0.8));
		classifier.buildClassifier(data2.x());

//		CrossValidation cv = new CrossValidation(classifier);

//		Map<Object, PerformanceMeasure> map = cv.crossValidation(data, 10);

		Map<Object, PerformanceMeasure> map = EvaluateDataset.testDataset(classifier, data2.y());

		double accuracy = 0d;
		double index    = 0;

		for (Object obj : map.keySet()) {
			accuracy += map.get(obj).getAccuracy();
			index++;
		}

		return accuracy / index;
	}

	private void saveRun(String id, String algorithm, double result) {
		String query = "insert into result (fk_data_id, FK_ALGORTITHM_ID, ACCURACY)" +
				"select " + id + ",(select pk_id from algorithm where name = '" + algorithm + "'), " + result;

		Statement st;
		try {
			st = conn.createStatement();
			st.execute(query);
		} catch (SQLException e) {
		}
	}

	private String getOpenmlId(String id) throws NotFoundException {
		String query =
				"select openml_id from data where pk_id = " + id;

		Statement st;

		try {
			st = conn.createStatement();
			ResultSet res = st.executeQuery(query);

			res.next();

			int val = res.getInt("openml_id");

			return String.valueOf(val);

		} catch (SQLException e) {
			System.out.println("ERROR retrieving value for " + id);
		}

		throw new NotFoundException("ERROR retrieving value for " + id);
	}

	private List<String> getAlgorithms() {
//		String query = "select name from algorithm";
//
//		List<String> algorithms = new ArrayList<String>();
//
//		Statement st;
//		try {
//			st = conn.createStatement();
//			ResultSet resultSet = st.executeQuery(query);
//
//			while (resultSet.next()) {
//				algorithms.add(resultSet.getString("name"));
//			}
//
//		} catch (SQLException e) {
//		}

		String[] temp = {
				"weka.DecisionStump(1)"
				, "weka.IB1(1)"
				, "weka.IBk(4)"
				, "weka.J48(1)"
				, "weka.JRip(1)"
				, "weka.NaiveBayes(1)"
				, "weka.OneR(1)"
				, "weka.RandomForest(1)"
				, "weka.ZeroR(1)"
		};

		return Arrays.asList(temp);
	}

	private void connect() {
		try {
			System.out.println("Connecting to MySQL...");

			Class.forName(driver).newInstance();
			conn = DriverManager.getConnection(url+dbName,userName,password);

			System.out.println("Successfully connected to MySQL");
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
