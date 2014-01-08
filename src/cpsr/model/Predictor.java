/*
 *   Copyright 2012 William Hamilton
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package cpsr.model;

import java.util.ArrayList;
import java.util.HashMap;

import org.jblas.DoubleMatrix;

import cpsr.environment.components.Action;
import cpsr.environment.components.ActionObservation;
import cpsr.environment.components.Observation;
import cpsr.model.exceptions.PSRRuntimeException;

/**
 * Class defines methods used to perform prediction with PSR representation. 
 * 
 * @author William Hamilton
 */
public class Predictor implements IPredictor 
{
	protected APSR psr;

	boolean mStarCreated;
	DoubleMatrix mStar;

	/**
	 * Default constructor for inheritance
	 */
	public Predictor()
	{
		super();
	}
	
	/**
	 * Creates prediction object using specified PSR.
	 * 
	 * @param psr PSR representation to be used in prediction. 
	 */
	public Predictor(APSR psr)
	{
		this.psr = psr;
		mStarCreated = false;
		mStar = null;
	}

	/* (non-Javadoc)
	 * @see cpsr.model.IPredictor#getImmediateProb(cpsr.environment.components.Action, cpsr.environment.components.Observation)
	 */
	@Override
	public double getImmediateProb(Action act, Observation ob)
	{
		ActionObservation actob = new ActionObservation(act, ob);
		ArrayList<ActionObservation> test = new ArrayList<ActionObservation>();
		test.add(actob);

		DoubleMatrix testParam = computeTestParameter(test);

		double prob = testParam.mmul(psr.getPredictionVector().getVector()).get(0,0);
		
		if(prob > 1.0) prob = 1.0;
		if(prob < 0.0) prob = 0.0;
		return prob;
	}

	/* (non-Javadoc)
	 * @see cpsr.model.IPredictor#getImmediateProb(java.util.ArrayList, java.util.ArrayList)
	 */
	@Override
	public double getImmediateProb(ArrayList<Action> acts, ArrayList<Observation> obs)
	{
		ArrayList<ActionObservation> test = createTestFromActionObservationLists(acts, obs);

		DoubleMatrix testParam = computeTestParameter(test);
		
		double prob = testParam.mmul(psr.getPredictionVector().getVector()).get(0,0);
		if(prob > 1.0) prob = 1.0;
		if(prob < 0.0) prob = 0.0;
		return prob;
	}

	/* (non-Javadoc)
	 * @see cpsr.model.IPredictor#getKStepPredictionProb(cpsr.environment.components.Action, cpsr.environment.components.Observation, int)
	 */
	@Override
	public double getKStepPredictionProb(Action act, Observation ob, int k)
	{
		act.setData(this.psr.getDataSet());
		ob.setData(this.psr.getDataSet());
		ActionObservation actob = new ActionObservation(act, ob);
		ArrayList<ActionObservation> test = new ArrayList<ActionObservation>();
		test.add(actob);

		if(!mStarCreated) createMStar();

		DoubleMatrix mStarCopy;
		if(k < 0)
		{
			throw new PSRRuntimeException("Cannot predict into the past, use valid k");
		}
		else if (k == 0)
		{
			mStarCopy = DoubleMatrix.eye(mStar.getRows());
		}
		else
		{
			mStarCopy = raiseMStarToPower(k);

		}
			
		DoubleMatrix tempMatrix = computeTestParameter(test);

		tempMatrix = tempMatrix.mmul(mStarCopy);
		double prob = tempMatrix.mmul(psr.getPredictionVector().getVector()).get(0,0);
		if(prob > 1.0) prob = 1.0;
		if(prob < 0.0) prob = 0.0;
		return prob;
		
	}

	/* (non-Javadoc)
	 * @see cpsr.model.IPredictor#getKStepPredictionProb(java.util.ArrayList, java.util.ArrayList, int)
	 */
	@Override
	public double getKStepPredictionProb(ArrayList<Action> acts, ArrayList<Observation> obs, int k)
	{
		ArrayList<ActionObservation> test = createTestFromActionObservationLists(acts, obs);

		if(!mStarCreated) createMStar();
		
		DoubleMatrix mStarCopy;
		if(k < 0)
		{
			throw new PSRRuntimeException("Cannot predict into the past, use valid k");
		}
		else if (k == 0)
		{
			mStarCopy = DoubleMatrix.eye(mStar.getRows());
		}
		else
		{
			mStarCopy = raiseMStarToPower(k);

		}

		DoubleMatrix tempMatrix = computeTestParameter(test);

		tempMatrix = tempMatrix.mmul(mStarCopy);

		double prob =  tempMatrix.mmul(psr.getPredictionVector().getVector()).get(0,0);
		if(prob > 1.0) prob = 1.0;
		if(prob < 0.0) prob = 0.0;
		return prob;
		
	}

	/**
	 * Helper method computes the MStar parameter.
	 * 
	 */
	private void createMStar()
	{
		mStar = null;
		HashMap<ActionObservation, DoubleMatrix> maos = psr.getAOMats();

		boolean firstIt = true;
		for(DoubleMatrix mao  : maos.values())
		{
			if(firstIt)
			{
				mStar = mao.dup();
			}
			else
			{
				mStar = mStar.add(mao);
			}
			firstIt = false;
		}
		mStarCreated = true;
	}

	/**
	 * Helper method returns MStar raised to specified power. 
	 * 
	 * @param k The power to which MStar is raised. 
	 * @return MStar raised to specified power. 
	 */
	private DoubleMatrix raiseMStarToPower(int k)
	{
		DoubleMatrix mStarCopy = mStar.dup();
		for(int i = 1; i < k ; i++)
		{
			mStarCopy = mStar.mmul(mStarCopy);
		}

		return mStarCopy;
	}

	/**
	 * Helper method computes the matrix parameter associated with a test. 
	 * 
	 * @param test The test which we want the parameter for.
	 * @return The parameter associated with the test. 
	 */
	private DoubleMatrix computeTestParameter(ArrayList<ActionObservation> test)
	{
		int length = test.size();
		DoubleMatrix tempMao = null;
		DoubleMatrix tempMatrix = null;

		try
		{
			tempMao = psr.getAOMat(test.get(length-1));
			tempMatrix = tempMao;
			for(int i = length - 2; i >= 0; i--)
			{
				tempMatrix = tempMatrix.mmul(psr.getAOMat(test.get(i)));
			}
			tempMatrix = psr.getMinf().getVector().transpose().mmul(tempMatrix);
		}
		catch(PSRRuntimeException ex)
		{
			ex.printStackTrace();
			tempMatrix = new DoubleMatrix(1, psr.getMinf().getVector().getRows());
		}
		
		return tempMatrix;
	}

	/**
	 * Helper method creates test from seperate lists of actions and observations.
	 * 
	 * @param acts List of actions. 
	 * @param obs List of observations.
	 * @return Vector representing test formed by combining action and observation
	 * list in order. 
	 * @throws  
	 */
	private ArrayList<ActionObservation> createTestFromActionObservationLists(ArrayList<Action> acts, ArrayList<Observation> obs) 
	{
		if(acts.size() != obs.size())
		{
			try
			{
				throw new PSRRuntimeException("Tests for prediction must have equal number of actions and observations");
			}
			catch(PSRRuntimeException ex)
			{
				ex.printStackTrace();
			}
		}

		int i = 0;
		ActionObservation actobentry = null;
		ArrayList<ActionObservation> test = new ArrayList<ActionObservation>();
		for(Action act : acts)
		{
			act.setData(this.psr.getDataSet());
			actobentry = new ActionObservation(act, obs.get(i));
			i++;
			test.add(actobentry);
		}
		return test;
	}
}
