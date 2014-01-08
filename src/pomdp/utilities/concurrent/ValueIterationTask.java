package pomdp.utilities.concurrent;

import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.Logger;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class ValueIterationTask extends Task implements Runnable {

	private ValueIteration m_viAlgortihm;
	private LinearValueFunctionApproximation m_vValueFunction;
	private int m_cMaxSteps;
	private double m_dEpsilon;
	
	public ValueIterationTask( ValueIteration viAlgorithm, int cMaxSteps, double dEpsilon ){
		m_viAlgortihm = viAlgorithm;
		m_vValueFunction = viAlgorithm.getValueFunction();
		m_cMaxSteps = cMaxSteps;
		m_dEpsilon = dEpsilon;
	}
	
	public void addAlphaVector( AlphaVector avNew ){
		m_vValueFunction.addPrunePointwiseDominated( avNew );
	}
	
	@Override
	public void copyResults(Task processed) {
		// TODO Auto-generated method stub
	}

	@Override
	public void execute() {
		m_viAlgortihm.valueIteration( m_cMaxSteps, m_dEpsilon );
		Logger.getInstance().log( "ValueIterationTask", 0, "execute", "Value Iteraiton task terminated" );
	}

	@Override
	public Element getDOM(Document doc) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
	}
	public void terminate(){
		super.terminate();
		m_viAlgortihm.terminate();
	}
}
