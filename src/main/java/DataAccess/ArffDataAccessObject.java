package DataAccess;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.data.ARFFHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jared on 12/3/15.
 */
public class ArffDataAccessObject implements IDataAccessObject {

	private File    file;
	private Dataset data;

	public ArffDataAccessObject(File pFile) throws IOException {
		file = pFile;

		int classLoc = -1;

		for(String line : getAllLines(file)) {
			line = line.toLowerCase();
			if(line.contains("@attribute")) {
				classLoc++;
				if(line.contains("class")) {
					break;
				}
			}
		}
		data = ARFFHandler.loadARFF(file, classLoc);
	}
	public ArffDataAccessObject(File pFile, String targetAttribute) throws IOException {
		file = pFile;

		int classLoc = -1;

		for(String line : getAllLines(file)) {
			line = line.toLowerCase();
			if(line.contains("@attribute")) {
				classLoc++;
				if(line.contains(targetAttribute.toLowerCase())) {
					break;
				}
			}
		}
		data = ARFFHandler.loadARFF(file, classLoc);
	}

	public ArffDataAccessObject(String filePath) throws IOException {
		file = new File(filePath);

		int classLoc = -1;

		for(String line : getAllLines(file)) {
			if(line.contains("@ATTRIBUTE")) {
				classLoc++;
				if(line.contains("class")) {
					break;
				}
			}
		}
		data = ARFFHandler.loadARFF(file, classLoc);
	}
	public ArffDataAccessObject(String filePath, int classLoc) throws FileNotFoundException {
		file = new File(filePath);
		data = ARFFHandler.loadARFF(file, classLoc);
	}

	public int getCount() {
		return data.size();
	}

	private List<String> getAllLines(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));

		String line = null;
		List<String> lines = new ArrayList<String>();

		while ((line = br.readLine()) != null) {
			lines.add(line);
		}

		br.close();
		return lines;
	}

	public Instance getInstance(String id) { return getInstance(Integer.parseInt(id)); }

	public Object getLabel(String id) {return getLabel(Integer.parseInt(id));}

	public boolean hasLabel(String id) { return getLabel(Integer.parseInt(id)) != null; }

	public Dataset getDataset() { return data; }
	public Dataset getDataset(String[] ids) {
		Dataset dataset = new DefaultDataset();

		for(String id : ids) {
			dataset.add(getInstance(id));
		}
		return dataset;
	}

	//TODO implement saveInstance
	public void saveInstance(String id, Instance instance) {

	}

	private Instance getInstance(int id) {
		return data.instance(id);
	}

	private Object getLabel(int id) {
		return data.instance(id).classValue();
	}
}
