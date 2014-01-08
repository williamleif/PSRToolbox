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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jblas.DoubleMatrix;

import cpsr.environment.DataSet;
import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.ActObSequenceSet;
import cpsr.environment.components.Action;
import cpsr.environment.components.ActionObservation;
import cpsr.model.components.Minf;
import cpsr.model.components.PredictionVector;
import cpsr.model.exceptions.PSRRuntimeException;
import cpsr.model.exceptions.PVException;

/**
 * Top-level class for learning and maintaining a PSR.
 * 
 * @author William Hamilton
 */
@SuppressWarnings("serial")
public abstract class APSR implements Serializable, IPSR
{
	protected PredictionVector pv, initialPv;
	protected TrainingDataSet trainData;
	protected Minf mInf;
	protected boolean isBuilt;
	protected ActObSequenceSet tests, histories;
	protected HashMap<ActionObservation, DoubleMatrix> aoMats;
	public List<List<Double>> info;
	private DoubleMatrix randomVec;
	

	/**
	 * Constructor initializes PSR of specified psrType with DataSet 
	 * 
	 * @param data The DataSet which the PSR is used to model.
	 * @param maxHistoryLength Max history length.
	 */
	protected APSR(TrainingDataSet data)
	{
		this.trainData = data;
		this.tests = data.getTests();
		this.histories = data.getHistories();
		info = new ArrayList<List<Double>>();
	}
	
	@Override
	public void build()
	{
		performBuild();
		isBuilt = true;
		initialPv = new PredictionVector(pv.getVector().dup());
		randomVec = DoubleMatrix.randn(5, 1);
	}
	
	protected abstract void performBuild();

	/* (non-Javadoc)
	 * @see cpsr.model.IPSR#isBuilt()
	 */
	@Override
	public boolean isBuilt()
	{
		return isBuilt;
	}

	/* (non-Javadoc)
	 * @see cpsr.model.IPSR#getDataSet()
	 */
	@Override
	public DataSet getDataSet()
	{
		return trainData;
	}
	
	public void update()
	{
		performUpdate();
		initialPv = new PredictionVector(pv.getVector().dup());
	}
	
	protected abstract void performUpdate();

	/* (non-Javadoc)
	 * @see cpsr.model.IPSR#update(cpsr.environment.components.ActionObservation)
	 */
	@Override
	public void update(ActionObservation ao)
	{
		DoubleMatrix tempMao = aoMats.get(ao);
		DoubleMatrix numerator, denominator;

		try
		{
			ArrayList<Double> tuple = new ArrayList<Double>();
			tuple.add((randomVec.transpose().mmul(tempMao)).norm2());
			tuple.add(pv.getVector().norm2());
			
			numerator = (tempMao.mmul(pv.getVector()));
			tuple.add(((randomVec.transpose()).mmul(numerator)).norm2());
			
			info.add(tuple);
		}
		catch(NullPointerException ex)
		{
			System.err.print("Unknown observation: no update performed");
			return;
		}
		denominator = (mInf.getVector().transpose()).mmul(numerator);

		try 
		{
			if(!(denominator.get(0, 0) == 0.0)) pv = new PredictionVector(numerator.mul((1.0/denominator.get(0,0))));
		} catch (PVException e) 
		{	
			e.printStackTrace();
			System.exit(0);
		}
	}

	/* (non-Javadoc)
	 * @see cpsr.model.IPSR#resetToStartState()
	 */
	@Override
	public void resetToStartState()
	{
		try
		{
			pv = new PredictionVector(initialPv.getVector().dup());
		}
		catch(PVException ex)
		{
			ex.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see cpsr.model.IPSR#getActionSet()
	 */
	@Override
	public HashSet<Action> getActionSet()
	{
		return this.trainData.getActionSet();
	}

	/**
	 * Returns deep copy of prediction vector.
	 * 
	 * @return Deep copy of prediction vector.
	 */
	public PredictionVector getPredictionVector()
	{
		try
		{
			return new PredictionVector(pv.getVector());
		}
		catch(PVException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns reference to Minf parameter.
	 * 
	 * @return Reference to Minf parameter.
	 */
	public Minf getMinf()
	{
		return mInf;
	}

	/**
	 * Returns reference to specified Mao parameter matrix. 
	 * 
	 * @param actob The action observation pair used to specify the Mao.
	 * @return The specified Mao parameter matrix. 
	 * @throws PSRRuntimeException
	 */
	public DoubleMatrix getAOMat(ActionObservation actob) throws PSRRuntimeException
	{
		DoubleMatrix mao = aoMats.get(actob);

		if(mao == null)
		{
			throw new PSRRuntimeException("There is no parameter associated with this action-observation pair");
		}
		else
		{
			return mao;
		}

	}

	/**
	 * Returns hash map of Mao parameters. 
	 * 
	 * @return Hash map of Mao parameters. 
	 */
	public HashMap<ActionObservation, DoubleMatrix> getAOMats()
	{
		return aoMats;
	}
	

}







