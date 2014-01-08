package pomdp.utilities.distance;

import pomdp.utilities.BeliefState;

public interface DistanceMetric {
	public double distance( BeliefState bs1, BeliefState bs2 );
}
