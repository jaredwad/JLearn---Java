package InstanceSelecters;

import net.sf.javaml.core.Instance;

import java.util.HashMap;
import java.util.Random;

/**
 * Created by jared on 12/10/15.
 */
public class RandomInstanceSelecter extends AInstanceSelecter {
	Random rand;

	public RandomInstanceSelecter() {
		rand = new Random();
	}

	@Override
	public String getNext() {
		HashMap<String, Instance> unlabeledData = learner.getUnlabeledData();

		int size = unlabeledData.size();

		if(size <= 0)
			return "";

		int i = -1;

		if(size == 1) {
			i = 0;
		} else {
			i = rand.nextInt(size - 1);
		}

		String nextID = "";

		System.out.print("Random Number: " + i);

		for(String key : unlabeledData.keySet()) {
			if(i <= 0) {
				nextID = key;
				break;
			}
			i--;
		}

		System.out.println(" -> Instance: " + nextID);
		return nextID;
	}
}
