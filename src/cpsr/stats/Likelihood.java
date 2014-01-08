/*
 *   Copyright 2013 William Hamilton
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
package cpsr.stats;

import java.util.ArrayList;
import java.util.List;

import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.Action;
import cpsr.environment.components.ActionObservation;
import cpsr.environment.components.Observation;
import cpsr.model.IPSR;
import cpsr.model.IPredictor;

/**
 * Class for obtaining likelihood of DataSets given PSR models.
 * 
 * @author William Hamilton
 */
public class Likelihood 
{
	
	IPSR aPsr;
	TrainingDataSet aData;
	IPredictor aPredictor;

	
	/**
	 * Constructs a likelihood object given psr model data and predictor.
	 * 
	 * @param pPsr A PSR model.
	 * @param pData A DataSet.
	 * @param pPredictor A prediction object defined over the PSR model.
	 */
	public Likelihood(IPSR pPsr, TrainingDataSet pData, IPredictor pPredictor)
	{
		aPsr = pPsr;
		aData = pData;
		aPredictor = pPredictor;
	}

	/**
	 * @return The likelihood of data given the PSR model.
	 */
	public double getLikelihoodOfData()
	{
		double lLikelihood;
		
		lLikelihood = 0.0;
		for(int i = 0; i < aData.getNumberOfRunsInBatch(); i++)
		{
			double lRunLike = 1.0;
			boolean firstStep = true;
			while(true)
			{
				if(firstStep)
				{
					firstStep = false;
				}
				else
				{
					if(checkForReset()) break;
				}
				ActionObservation ao = aData.getNextActionObservationForPlanning();
				lRunLike*=aPredictor.getImmediateProb(ao.getAction(),
						ao.getObservation());
				aPsr.update(ao);
	
			}
			lLikelihood+=lRunLike;
		}
		
		lLikelihood = lLikelihood/((double)aData.getNumberOfRunsInBatch());
		
		return lLikelihood;
	}
	
	/**
	 * @return The likelihood of data given the PSR model.
	 */
	public List<double[]> getKStepLikelihoodsOfData(int k)
	{
		
		List<List<Double>> likehoods = new ArrayList<List<Double>>(); 
		
		for(int i = 0; i < k; i++)
		{
			likehoods.add(new ArrayList<Double>());
		}
		
		for(int i = 0; i < aData.getNumberOfRunsInBatch(); i++)
		{
			double lRunLike = 1.0;
			boolean firstStep = true;
			int iterCount = 0;
			while(true)
			{
				iterCount++;
				if(firstStep)
				{
					firstStep = false;
				}
				else
				{
					if(checkForReset() || iterCount > k) break;
				}
				ActionObservation ao = aData.getNextActionObservation();
				
				if(ao.getID() == -1)
					break;
				
				lRunLike*=aPredictor.getImmediateProb(ao.getAction(),
						ao.getObservation());
				likehoods.get(iterCount-1).add(lRunLike);
				aPsr.update(ao);
			}
		}
		
		List<double[]> results = new ArrayList<double[]>();
		for(List<Double> kLike : likehoods)
		{
			double[] res = new double[2];
			
			res[0] = Basic.mean(kLike);
			res[1] = Basic.std(kLike, res[0]);
			
			results.add(res);
		}
		
		return results;
	}
	
	/**
	 * @return The likelihood of the first observations given the PSR model.
	 */
	public double getOneStepLikelihoodOfData()
	{
		double lLikelihood;
		
		lLikelihood = 0.0;
		for(int i = 0; i < aData.getNumberOfRunsInBatch(); i++)
		{
				Action nextAct = aData.getNextActionObservationForPlanning().getAction();
				Observation nextObs = aData.getNextActionObservationForPlanning().getObservation();
				lLikelihood+=aPredictor.getImmediateProb(nextAct, nextObs);
				aPsr.update(new ActionObservation(nextAct, nextObs));
	
		}
		
		lLikelihood = lLikelihood/((double)aData.getNumberOfRunsInBatch());
		
		return lLikelihood;
	}
	
	/**
	 * Helper method determines if a run terminated.
	 * If so, true returned and prediction vector reset. 
	 * 
	 * @return Boolean representing whether reset performed.
	 */
	private boolean checkForReset()
	{
		if(aData.resetPerformed())
		{
			aPsr.resetToStartState();
			return true;
		}
		else
		{
			return false;
		}
	}
}
