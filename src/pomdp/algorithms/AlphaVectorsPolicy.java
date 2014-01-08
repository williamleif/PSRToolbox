package pomdp.algorithms;

import pomdp.utilities.BeliefState;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class AlphaVectorsPolicy extends PolicyStrategy {

	protected LinearValueFunctionApproximation m_vValueFunction;
	
	public AlphaVectorsPolicy(){
		m_vValueFunction = null;
	}
	
	@Override
	public int getAction(BeliefState bsCurrent) {
		return m_vValueFunction.getBestAction( bsCurrent );
	}

	@Override
	public String getStatus() {
		return null;
	}

	@Override
	public double getValue( BeliefState bsCurrent ) {
		return m_vValueFunction.valueAt( bsCurrent );
	}

	@Override
	public LinearValueFunctionApproximation getValueFunction() {
		return m_vValueFunction;
	}

	public void setValueFunction( LinearValueFunctionApproximation vValueFunction ) {
		m_vValueFunction = vValueFunction;
	}

	@Override
	public boolean hasConverged() {
		return m_vValueFunction != null;
	}

}
