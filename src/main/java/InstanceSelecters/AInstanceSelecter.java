package InstanceSelecters;

import Learners.PoolLearner;

/**
 * Created by jared on 12/3/15.
 */
public abstract class AInstanceSelecter {
	PoolLearner learner;

	public abstract String getNext();

	public void setLearner(PoolLearner pLearner) { learner = pLearner; }
}
