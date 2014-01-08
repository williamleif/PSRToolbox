package pomdp.valuefunction;

import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;

public class DicretizedUpperBound extends
		UpperBoundValueFunctionApproximation {

	private int m_cDiscretizationLevels;
	
	public DicretizedUpperBound( POMDP pomdp, int cDiscretizationLevels, MDPValueFunction vfMDP ){
		super( pomdp, vfMDP, true );
		m_cDiscretizationLevels = cDiscretizationLevels;
	}
	
	public void updateValue( BeliefState bs ) {
		BeliefState bsDiscretized = m_pPOMDP.getBeliefStateFactory().discretize( bs, m_cDiscretizationLevels );
		super.updateValue( bsDiscretized );
	}

	public double valueAt( BeliefState bs ) {
		BeliefState bsDiscretized = m_pPOMDP.getBeliefStateFactory().discretize( bs, m_cDiscretizationLevels );
		return super.valueAt( bsDiscretized );
	}

}
