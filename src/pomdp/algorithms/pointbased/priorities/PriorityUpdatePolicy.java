package pomdp.algorithms.pointbased.priorities;

import java.util.Vector;

import pomdp.utilities.BeliefState;
import pomdp.utilities.datastructures.PriorityQueue;

public interface PriorityUpdatePolicy {
	public void updatePriorities( BeliefState bsCurrent, 
			double dDelta, double dEpsilon, 
			Vector vBeliefStates, PriorityQueue pq, double dGamma );
}
