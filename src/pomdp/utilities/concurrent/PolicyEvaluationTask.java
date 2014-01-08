package pomdp.utilities.concurrent;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pomdp.algorithms.PolicyStrategy;
import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.Logger;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class PolicyEvaluationTask extends Task implements Runnable {

	private LinearValueFunctionApproximation m_vValueFunction;
	private POMDP m_pPOMDP;
	private int m_cTrials, m_cSteps, m_cEvaluations, m_iTimeBetweenEvaluations;
	private String m_sMethods;
	
	public PolicyEvaluationTask( POMDP pomdp, LinearValueFunctionApproximation vValueFunction, String sMethods, int cEvaluations, int iTimeBetweenEvaluations, int cTrials, int cTrialLength ){
		m_pPOMDP = pomdp;
		m_vValueFunction = vValueFunction;
		m_cTrials = cTrials;
		m_cSteps = cTrialLength;
		m_cEvaluations = cEvaluations;
		m_iTimeBetweenEvaluations = iTimeBetweenEvaluations;
		m_sMethods = sMethods;
	}
	
	
	@Override
	public void copyResults(Task processed) {
		// TODO Auto-generated method stub

	}

	@Override
	public void execute() {
		double dADR = 0.0;
		long lTimeBefore = 0, lTimeAfter = 0, lStartTime = 0;
		String sMessage = "";
		PolicyStrategy psStrategy = null;
		int i = 0;
		try{
			FileWriter fwADR = new FileWriter( m_pPOMDP.getName() + "ParallelExecution.txt", true );
			fwADR.write( m_sMethods + "\t" );
			FileWriter fwVectors = new FileWriter( m_pPOMDP.getName() + "VectorPruning.txt", true );
			fwVectors.write( m_sMethods + "\t" );
			
			
			for( i = 0 ; i < m_cEvaluations && !m_bTerminate ; i++ ){
				lStartTime = System.currentTimeMillis();
				
				try {
					synchronized( this ){
						wait( m_iTimeBetweenEvaluations );
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				/*
				while( ( lStartTime - lTimeBefore ) < 5000 ){
					try {
						synchronized( this ){
							wait( 1000 );
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					lStartTime = System.currentTimeMillis();
				}
				*/
				Runtime rt = Runtime.getRuntime();
				m_vValueFunction.startEvaluation();
				rt.gc();
				try {
					wait( 1000 );
				} catch (Exception e) {
				}
				if( ( rt.maxMemory() / 1000000 > rt.totalMemory() / 1000000 ) || ( rt.freeMemory() / 1000000 >= 200 ) ){					
					lTimeBefore = System.currentTimeMillis();
					if( true || m_vValueFunction.size() > 30 ){
						Logger.getInstance().log( "PolicyEvaluationTask", 0, "execute", "Start policy evaluation" );
						psStrategy = new PolicyWrapper( new LinearValueFunctionApproximation( m_vValueFunction ) );
						dADR = m_pPOMDP.computeAverageDiscountedReward( m_cTrials, m_cSteps, psStrategy, true, true );
					}
					else{
						dADR = 0.0;
					}
					lTimeAfter = System.currentTimeMillis();
					sMessage = "Iteration " + i + " Time " + ( lTimeAfter - lStartTime ) / 1000 + " ADR " + dADR;
					fwADR.write( dADR + "\t" );
					fwADR.flush();
					fwVectors.write( m_vValueFunction.size() + "\t" );
					fwVectors.flush();
					Logger.getInstance().log( "PolicyEvaluationTask", 0, "execute", sMessage );
				}
				else{
					m_bTerminate = true;
				}
				m_vValueFunction.endEvaluation();
			}
			
			fwADR.write( "\n" );
			fwADR.close();
			fwVectors.write( "\n" );
			fwVectors.close();
		}
		catch( IOException e ){
			e.printStackTrace();
		}
		Logger.getInstance().log( "PolicyEvaluationTask", 0, "execute", "Done evaluation - exiting" );
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

	private class PolicyWrapper extends PolicyStrategy{
		private LinearValueFunctionApproximation m_vValueFunction;

		public PolicyWrapper( LinearValueFunctionApproximation vValueFunction ) {
			m_vValueFunction = vValueFunction;
		}

		@Override
		public int getAction(BeliefState bsCurrent) {
			return m_vValueFunction.getBestAction( bsCurrent );
		}

		@Override
		public String getStatus() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double getValue(BeliefState bsCurrent) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public LinearValueFunctionApproximation getValueFunction() {
			return m_vValueFunction;
		}

		@Override
		public boolean hasConverged() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}
