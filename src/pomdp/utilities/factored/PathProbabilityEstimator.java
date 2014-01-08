package pomdp.utilities.factored;

import java.util.Vector;

import pomdp.utilities.Pair;

public interface PathProbabilityEstimator {
	double valueAt( Vector<Pair<Integer, Boolean>> vAssignment );
}
