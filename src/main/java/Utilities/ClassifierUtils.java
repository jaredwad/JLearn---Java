package Utilities;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.tools.weka.WekaClassifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jared on 12/9/15.
 */
public class ClassifierUtils {

	public static HashMap<String, Classifier> createClassifiers(List<String> classifierList) {
		HashMap<String, Classifier> classifiers = new HashMap<String, Classifier>();

		for(String classifierName : classifierList) {


		}

		System.out.println("Finished creating classifiers");

		return classifiers;
	}

	public static Classifier createClassifier(String setupString) throws Exception {
		String[] options;

		try {

			String[] split = setupString.split("--");

			String classifierName = split[0].trim();

			if(split.length > 1)
				options = weka.core.Utils.splitOptions(setupString);
			else
				options = new String[0];


//			System.out.println("Attempting to create classifier " + classifierName);
//			System.out.println("\t with options: ");

			for (int i = 0; i < options.length; ++i) {
				if (options[i].equals("--")) {
					options[i] = "";
					break;
				}
				options[i] = "";
			}

			List<String> optionsList = new ArrayList<String>();

			for(int i = 0; i < options.length; ++i) {
				if(!StringUtils.isNullOrWhiteSpaceOrSpecial(options[i])) {
//					System.out.println("\t\t" + options[i]);
					optionsList.add(options[i]);
				}
			}

			return createClassifier(classifierName, optionsList.toArray(new String[optionsList.size()]));

		} catch (Exception e) {
			System.out.println("\tError creating classifier " + setupString);
		}

		throw new Exception("Error creating classifier " + setupString);
	}

	public static Classifier createClassifier(String classifierName, String[] options) throws Exception {
		weka.classifiers.Classifier wekaClassifier = weka.classifiers.Classifier.forName(classifierName, options);

		return new WekaClassifier(wekaClassifier);
	}
}
