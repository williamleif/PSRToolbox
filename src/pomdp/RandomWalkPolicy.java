package pomdp;

import pomdp.algorithms.PolicyStrategy;
import pomdp.utilities.BeliefState;
import pomdp.utilities.RandomGenerator;
import pomdp.valuefunction.LinearValueFunctionApproximation;

/**
 * @author shanigu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

public class RandomWalkPolicy extends PolicyStrategy {
	private int m_cActions;
	protected RandomGenerator m_rndGenerator;
	
	public RandomWalkPolicy( int cActions ){
		m_cActions = cActions;
		m_rndGenerator = new RandomGenerator( "RandomWalk" );
	}
	 
	/* (non-Javadoc)
	 * @see PolicyStrategy#getAction(BeliefState)
	 */
	public int getAction( BeliefState bsCurrent ){
		return m_rndGenerator.nextInt( m_cActions );
	}

	public double getValue(BeliefState bsCurrent) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean hasConverged() {
		// TODO Auto-generated method stub
		return false;
	}

	public String getStatus() {
		return "N/A";
	}

	public LinearValueFunctionApproximation getValueFunction() {
		// TODO Auto-generated method stub
		return null;
	}

}
