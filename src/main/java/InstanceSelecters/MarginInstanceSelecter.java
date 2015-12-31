package InstanceSelecters;

import Learners.PoolLearner;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;

import java.util.*;

/**
 * Created by jared on 12/3/15.
 */
public class MarginInstanceSelecter extends AInstanceSelecter {

	@Override
	public String getNext() {
		if(learner.getLabeledSize() <= 0) {
			return getRandom();
		}


		double minMargin = Double.MAX_VALUE;
		int index = 0;

		Dataset unlabeledData = learner.getUnlabledAsDataset();
		Classifier classifier = learner.getClassifier();

		int size = unlabeledData.size();
		for(int i = 0; i < size; ++i) {
			Map<Object, Double> map = classifier.classDistribution(unlabeledData.get(i));

			double margin = getMargin(map);

			if(margin < minMargin) {
				minMargin = margin;
				index = i;
			}
		}
		return idFromIndex(index);
	}

	private double getMargin(Map<Object, Double> map) {
		int size = map.size();

		if(size <= 1)
			return 1;

		ArrayList<Double> items = new ArrayList<Double>(size);

		double total = 0;

		for(Object key : map.keySet()) {
			total += map.get(key);
		}
		for(Object key : map.keySet()) {
			items.add(map.get(key) / total);
		}

		Collections.sort(items);

		return items.get(0) - items.get(1);
	}

	private String getRandom() {
		int size = learner.getUnlabeledSize();

		if(size <= 0)
			return "";

		Random rand = new Random();

		return idFromIndex(rand.nextInt(size - 1));
	}

	private String idFromIndex(String index) {
		int i = Integer.parseInt(index);
		return idFromIndex(i);
	}

	private String idFromIndex(int index) {
		HashMap<String, Instance> unlabeledData = learner.getUnlabeledData();
		String nextID = "";

		for(String key : unlabeledData.keySet()) {
			if(index <= 0) {
				nextID = key;
				break;
			}
			index--;
		}

		return nextID;
	}
}
