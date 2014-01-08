package pomdp.algorithms.pointbased.priorities;

import java.util.Iterator;
import java.util.Vector;

import pomdp.utilities.BeliefState;
import pomdp.utilities.datastructures.PriorityQueue;

public class ExplorationBasedPriorityUpdate implements PriorityUpdatePolicy {
	
	public ExplorationBasedPriorityUpdate(){
	}
	
	public void updatePriorities( BeliefState bsCurrent, double dDelta,
			double dEpsilon, Vector vBeliefStates, PriorityQueue pq, double dGamma ){
		Iterator itPredecessors = bsCurrent.getPredecessors().iterator();
		BeliefState bsPredecessor = null;
		double dProb = 0.0;
		
		if( dDelta < dEpsilon )
			return;
		
		while( itPredecessors.hasNext() ){
			bsPredecessor = (BeliefState) itPredecessors.next();
			dProb = bsCurrent.getProbCurrentGivenPredecessor( bsPredecessor );
			
			
			if( vBeliefStates.contains( bsPredecessor ) ){
				if( bsPredecessor.getLocation() == -1 ){
					bsPredecessor.setPriority( 0.0 );
					pq.insert( bsPredecessor );
				}
			
				bsPredecessor.increasePriority( dDelta * dProb * dGamma );
			}
		}
	}

}
