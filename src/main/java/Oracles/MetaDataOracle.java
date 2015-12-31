package Oracles;

import DataAccess.ArffDataAccessObject;
import DataAccess.IDataAccessObject;
import be.abeel.util.Pair;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.evaluation.EvaluateDataset;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.sampling.Sampling;
import net.sf.javaml.tools.weka.WekaClassifier;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Created by jared on 12/3/15.
 */
public class MetaDataOracle implements IOracle {
	//	ArffDataAccessObject adao;
	IDataAccessObject dao;


	HashMap<String, Classifier>        classifiers;
	//	HashMap<String, Object    > labels;
	HashMap<String, IDataAccessObject> datasets;

	public MetaDataOracle(IDataAccessObject pDao
			, HashMap<String, Classifier> pClassifiers
			, HashMap<String, IDataAccessObject> pDatasets) {
		dao = pDao;
		classifiers = pClassifiers;
		datasets = pDatasets;
	}

	public void setClassifiers(HashMap<String, Classifier> pClassifiers) { classifiers = pClassifiers; }

//	private boolean DbHasInstance(String id) { return dao.hasLabel(id); }

	public Object getLabel(String id) {
		Object label = getLabelFromDB(id);

		if (label == null || label.equals("")) {
//			try {
//				return getLabelFromTests(id);
//			} catch (IOException e) {
				return null;
//			}
		}
		return label;
	}

	private Object getLabelFromDB(String id) {
//		Instance instance = dao.getInstance(id);
		return dao.getLabel(id);
//		return instance;
	}

	private Object getLabelFromTests(String id) throws IOException {
		IDataAccessObject idao = datasets.get(id);

		Dataset dataset = idao.getDataset();

		double bestAccuracy   = 0;
		String bestClassifier = null;

		for (String classifier : classifiers.keySet()) {
			double accuracy = runClassifier(classifiers.get(classifier), dataset);

			if (accuracy > bestAccuracy) {
				bestClassifier = classifier;
				bestAccuracy = accuracy;
			}
		}

		Instance instance = dao.getInstance(id);
		instance.setClassValue(bestClassifier);
		dao.saveInstance(id, instance);

		return bestClassifier;
	}

	private double runClassifier(Classifier classifier, Dataset data) {
		Pair<Dataset, Dataset> data2 = Sampling.SubSampling.sample(data, (int) (data.size() * 0.8));

		classifier.buildClassifier(data2.x());

		Map<Object, PerformanceMeasure> map = EvaluateDataset.testDataset(classifier, data2.y());

		double accuracy = 0d;
		double index    = 0;

		for (Object obj : map.keySet()) {
			accuracy += map.get(obj).getAccuracy();
			index++;
		}

		return accuracy / index;
	}
}