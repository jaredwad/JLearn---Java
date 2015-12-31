package Utilities;

import DataAccess.ArffDataAccessObject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.DataSetDescription;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jared on 12/9/15.
 */
public class OpenMLUtils {
	private static String hash = "54552c2da5b4c277ef2539a5d03b61bd";

	private static OpenmlConnector client = new OpenmlConnector(hash);

	public static ArffDataAccessObject getArffDAO(String id) {

		try {
			DataSetDescription dataSetDescription = client.dataGet(Integer.parseInt(id));

//			System.out.println("Attempting to get ArffDAO for dataset: " + dataSetDescription.getName());

			File dataset = dataSetDescription.getDataset(hash);

			String default_target_attribute = dataSetDescription.getDefault_target_attribute();

			if(default_target_attribute != null)
				return new ArffDataAccessObject(dataset, default_target_attribute);
			else
				return new ArffDataAccessObject(dataset);
		} catch (Exception e) {
			System.out.println("Error retrieving dataset with id: " + id);
//			e.printStackTrace();
		}

		return null;
	}

	public static int[] getIds(int maxNumInstances) {
		int[] ids = null;

		String query = "select distinct d.did, d.name, q.value from dataset as d" +
				" Join data_quality as q" +
				" on d.did = q.data" +
				" where q.quality='NumberOfInstances'" +
				" and q.value < " + maxNumInstances +
				" order by d.did";
		try {
			JSONObject jsonObject = client.freeQuery(query);

			JSONArray data = jsonObject.getJSONArray("data");

			int length = data.length();

//			length = 100; //TODO remove this

			ids = new int[length];

			for(int i = 0; i < length; ++i) {
				ids[i] = data.getJSONArray(i).getInt(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ids;
	}


}
