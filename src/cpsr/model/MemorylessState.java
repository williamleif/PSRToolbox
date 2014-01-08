package cpsr.model;

import java.util.HashMap;
import java.util.HashSet;

import org.jblas.DoubleMatrix;

import cpsr.environment.DataSet;
import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.Action;
import cpsr.environment.components.ActionObservation;
import cpsr.environment.components.Observation;
import cpsr.model.components.Minf;
import cpsr.model.components.PredictionVector;
import cpsr.model.exceptions.PSRRuntimeException;
import cpsr.stats.PSRObserver;

public class MemorylessState extends APSR {

	private static final long serialVersionUID = -2790344407555914392L;
	
	Observation lastOb;
	/**
	 * Constructor initializes PSR of specified psrType with DataSet 
	 * 
	 * @param data The DataSet which the PSR is used to model.
	 * @param maxHistoryLength Max history length.
	 */
	public MemorylessState(TrainingDataSet data)
	{
		super(data);
		DoubleMatrix initVec = new DoubleMatrix(1,1);
		initVec.put(0,0, -1);
		initialPv = new PredictionVector(initVec);
	}
	
	@Override
	public void build()
	{
		performBuild();
		isBuilt = true;
	}
	

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
	}
	

	/* (non-Javadoc)
	 * @see cpsr.model.IPSR#update(cpsr.environment.components.ActionObservation)
	 */
	@Override
	public void update(ActionObservation ao)
	{
		lastOb = ao.getObservation();
	}

	/* (non-Javadoc)
	 * @see cpsr.model.IPSR#resetToStartState()
	 */
	@Override
	public void resetToStartState()
	{
		lastOb = null;
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
		if(lastOb == null)
		{
			return initialPv;
		}
		else
		{
			DoubleMatrix obVec = new DoubleMatrix(1,1);
			obVec.put(0,0, lastOb.getID());
			return new PredictionVector(obVec);
		}
	}

	/**
	 * Returns reference to Minf parameter.
	 * 
	 * @return Reference to Minf parameter.
	 */
	public Minf getMinf()
	{
		return null;
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
		return null;
	}

	/**
	 * Returns hash map of Mao parameters. 
	 * 
	 * @return Hash map of Mao parameters. 
	 */
	public HashMap<ActionObservation, DoubleMatrix> getAOMats()
	{
		return null;
	}
	@Override
	public void addPSRObserver(PSRObserver observer) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void performBuild() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void performUpdate() {
		// TODO Auto-generated method stub

	}

}
