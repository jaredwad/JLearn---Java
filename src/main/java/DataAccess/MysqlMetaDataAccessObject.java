package DataAccess;

import javassist.NotFoundException;
import net.sf.javaml.core.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jared on 12/8/15.
 */
public class MysqlMetaDataAccessObject implements IDataAccessObject {
	String url = "jdbc:mysql://localhost:8889/";
	String dbName = "openML";
	String driver = "com.mysql.jdbc.Driver";
	String userName = "root";
	String password = "cocobear1";
	Connection conn;

	List<String> algorithms;
	List<String> qualities;

	public MysqlMetaDataAccessObject() {
		connect();
		qualities = getQualities();
	}

	public MysqlMetaDataAccessObject(String pUrl, String pDbName, String pDriver
			, String pUserName, String pPassword, List<String> pAlgorithms) {
		System.out.println("Creating MysqlMetaDataAccessObject");

		url      = pUrl;
		dbName   = pDbName;
		driver   = pDriver;
		userName = pUserName;
		password = pPassword;

		connect();

		algorithms = pAlgorithms;
		qualities = getQualities();
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
					query = "select * from data_quality as q" +
							" where q.data=" + id +
							" and q.quality='" + quality + "'";

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

	public Object  getLabel(String id) {
		System.out.println("Getting Label for id: " + id);

		String best = "";
		double highest = 0;

		for(String algorithm : algorithms) {
			try {
				double newValue = getValueForAlgorithm(id, algorithm);

				if(newValue > highest) {
					best = algorithm;
					highest = newValue;
				}

			} catch (NotFoundException e) {
				System.out.println("Couldn't find algorithm " + algorithm + " for id = " + id);
				continue; //TODO change this to run the algorithm
			}
		}

		System.out.println("The label is: " + best);
		return best;
	}
	public boolean hasLabel(String id) { return getLabel(id).equals(null); }

	public Dataset getDataset() {
		return null;
	}
	public Dataset getDataset(String[] ids) {
		Dataset data = new DefaultDataset();

		for(String id : ids) {
			Instance instance = getInstance(id);
			data.add(instance);
		}
		return data;
	}

	public void saveInstance(String id, Instance instance) {}

	private double getValueForAlgorithm(String id, String Algorithm) throws NotFoundException {
//		String[] split = Algorithm.split(".");
//		split = split[split.length-1].split("--");
//		Algorithm = split[0];

		System.out.println("Getting value for " + Algorithm);

		String query =
				"SELECT i.fullName, avg(e.value) as value\n" +
						"FROM algorithm_setup l, evaluation e, run r, dataset d, implementation i, task_inputs ti\n" +
						"WHERE r.setup = l.sid\n" +
						"AND l.isDefault = 'true'\n" +
						"AND r.task_id = ti.task_id\n" +
						"AND ti.input='source_data'\n" +
						"AND ti.value=d.did\n" +
						"AND l.implementation_id = i.id\n" +
						"AND d.isOriginal='true'\n" +
						"AND e.source=r.rid\n" +
						"AND e.function='predictive_accuracy'\n" +
						"AND d.did = " + id + "\n" +
						"and i.fullname LIKE '" + Algorithm + "'\n" +
						"group by i.fullname\n" +
						"ORDER BY e.value DESC;";

		Statement st = null;
		try {
			st = conn.createStatement();

			ResultSet res;

			res = st.executeQuery(query);

			double i = 0;
			double value = 0;

			while(res.next()) {
				double resVal = res.getDouble("value");

				if(resVal != Double.NaN) {
					value += resVal;
					++i;
				}
			}

			if(i > 0) {
				System.out.println("The result for " + Algorithm + " on data set " + id + " is:" + value / i);

				return value / i;
			}

			System.out.println("The result for " + Algorithm + " on data set " + id + " could not be found.");
			return 0;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		throw new NotFoundException("The result for " + Algorithm + " on data set " + id + " could not be found.");
	}

	private List<String> getQualities() {
		System.out.println("Getting possible data quality values from MySQL");

		List<String> qualities = new ArrayList<String>(100);
		Statement st = null;
		try {
			st = conn.createStatement();
			ResultSet res = st.executeQuery("select * from quality " +
													"where type = 'DataQuality'");

			while (res.next()) {
				String name = res.getString("name");
				System.out.println("Adding quality: " + name);
				qualities.add(name);
			}

		} catch (SQLException e) {
			System.out.println("ERROR retrieving data qualities");
		}

		System.out.println("Successfully retrieved values");

		return qualities;
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
