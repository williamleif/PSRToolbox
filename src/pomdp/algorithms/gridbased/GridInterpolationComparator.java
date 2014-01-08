package pomdp.algorithms.gridbased;

import java.util.Comparator;

import pomdp.utilities.BeliefState;

public class GridInterpolationComparator implements Comparator<BeliefState> {

	public int compare( BeliefState bs1, BeliefState bs2 ) {
		return bs1.getGridInterpolations() - bs2.getGridInterpolations();
	}

}
