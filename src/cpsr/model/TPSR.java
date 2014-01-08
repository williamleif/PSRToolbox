package cpsr.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.jblas.DoubleMatrix;
import org.jblas.Singular;
import org.jblas.Solve;

import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.ActionObservation;
import cpsr.model.components.Minf;
import cpsr.model.components.PredictionVector;
import cpsr.stats.PSRObserver;

public class TPSR extends APSR 
{

	private static final long serialVersionUID = 3166796096290306882L;

	public static final int UPPER_HIST_LEN_LIMIT = 1000000;
	public static final double STABILITY_CONSTANT = 1.0/1000.0;
	
	protected double minSingularVal;
	protected int maxDim, maxHistLen;
	
	protected DoubleMatrix hist, th, pseudoInverse;
	protected DoubleMatrix[] svdResults;
	
	protected List<PSRObserver> observers;

	protected int resetCount;

	
	public TPSR(TrainingDataSet trainData, double minSingularVal, int maxDim, int maxHistLen)
	{
		super(trainData);
		
		if(maxHistLen > UPPER_HIST_LEN_LIMIT)
			throw new IllegalArgumentException("Max history length of: " + maxHistLen + 
					" exceeds limit of: " + UPPER_HIST_LEN_LIMIT);
		
		this.minSingularVal = minSingularVal;
		this.maxDim = maxDim;
		this.maxHistLen = maxHistLen;
		observers = new LinkedList<PSRObserver>();
	}
	
	public TPSR(TrainingDataSet trainData, double minSingularVal, int maxDim)
	{
		super(trainData);
		this.minSingularVal = minSingularVal;
		this.maxDim = maxDim;
		this.maxHistLen = UPPER_HIST_LEN_LIMIT;
		observers = new LinkedList<PSRObserver>();
	}
	
	public void addPSRObserver(PSRObserver observer)
	{
		observers.add(observer);
	}
	
	protected void notifyPSRObservers(double[] singVals)
	{
		for(PSRObserver observer : observers)
		{
			observer.modelUpdated(singVals, tests, histories);
		}
	}
	
	@Override
	protected void performBuild() 
	{
		//initializing observable matrices
		hist = new DoubleMatrix(histories.size()+1, 1);
		th = new DoubleMatrix(tests.size(), histories.size()+1);
		estimateHistoryAndTHMatrices();
//		th.muli(STABILITY_CONSTANT);
//		hist.muli(STABILITY_CONSTANT);
		
		svdResults = computeTruncatedSVD(th, minSingularVal, maxDim);
		pseudoInverse = svdResults[2].mmul(Solve.pinv(svdResults[1]));
		trainData.resetData();
		aoMats = new HashMap<ActionObservation, DoubleMatrix>();
		for(ActionObservation actOb : trainData.getValidActionObservationSet())
		{
			aoMats.put(actOb, new DoubleMatrix(pseudoInverse.getColumns(), pseudoInverse.getColumns()));
		}
		constructAOMatrices();
//		for(ActionObservation actOb : trainData.getValidActionObservationSet())
//		{
//			aoMats.put(actOb, aoMats.get(actOb).muli(STABILITY_CONSTANT));
//		}
		resetCount=0;
		mInf = new Minf(((hist.transpose()).mmul(pseudoInverse)).transpose());
		pv = new PredictionVector(svdResults[0].mmul(th.getColumn(0)));
	}
	
	protected void performUpdate()
	{
		hist = DoubleMatrix.concatVertically(hist, 
				DoubleMatrix.zeros(histories.size()+1-hist.getRows(),1));
		th = DoubleMatrix.zeros(th.getRows(), histories.size()+1);
	
		estimateHistoryAndTHMatrices();
//		th.muli(STABILITY_CONSTANT);
//		hist.muli(STABILITY_CONSTANT);
		
		DoubleMatrix oldU = svdResults[0].transpose().dup();
		DoubleMatrix oldS = svdResults[1].dup();
		DoubleMatrix oldV = svdResults[2].dup();
		svdResults = updateSVD(th);
		oldV = DoubleMatrix.concatVertically(oldV, DoubleMatrix.zeros(svdResults[2].getRows()-oldV.getRows(), oldV.getColumns()));
		pseudoInverse = svdResults[2].mmul(Solve.pinv(svdResults[1]));
		for(ActionObservation actOb : trainData.getValidActionObservationSet())
		{
			if(!aoMats.keySet().contains(actOb))
			{
				aoMats.put(actOb, new DoubleMatrix(pseudoInverse.getColumns(), pseudoInverse.getColumns()));
			}
			else
			{
				aoMats.put(actOb, (svdResults[0].mmul(oldU).mmul(aoMats.get(actOb)).mmul(oldS).mmul(oldV.transpose()).mmul(pseudoInverse)));
			}
		}
		trainData.resetData();
		
		constructAOMatrices();
//		for(ActionObservation actOb : trainData.getValidActionObservationSet())
//		{
//			aoMats.put(actOb, aoMats.get(actOb).muli(1.0/((double)resetCount)));
//		}
		resetCount=0;
		mInf = new Minf(((hist.transpose()).mmul(pseudoInverse)).transpose());
		pv = new PredictionVector(svdResults[1].mmul(svdResults[2].transpose()).getColumn(0));
	}
	
	protected DoubleMatrix[] updateSVD(DoubleMatrix newTHData)
	{
		DoubleMatrix biggerV = DoubleMatrix.concatVertically(svdResults[2],
				DoubleMatrix.zeros(newTHData.getColumns()-svdResults[2].getRows(), svdResults[2].getColumns()));
		
		DoubleMatrix m = svdResults[0].mmul(newTHData);
		DoubleMatrix p = newTHData.sub((svdResults[0].transpose()).mmul(m));
		
		DoubleMatrix pBase = Singular.sparseSVD(p)[0];
		pBase = DoubleMatrix.concatHorizontally(pBase, DoubleMatrix.zeros(pBase.getRows(), p.getColumns()-pBase.getColumns()));
		DoubleMatrix rA = (pBase.transpose()).mmul(p);
		
		DoubleMatrix n = (biggerV.transpose()).mmul(DoubleMatrix.eye(newTHData.getColumns()));
		DoubleMatrix q = DoubleMatrix.eye(newTHData.getColumns()).sub(biggerV.mmul(n));
		
		DoubleMatrix qBase = Singular.sparseSVD(q)[0];
		DoubleMatrix rB = (qBase.transpose()).mmul(q);
		
		DoubleMatrix z = DoubleMatrix.zeros(m.getRows(), m.getColumns());
		DoubleMatrix z2 = DoubleMatrix.zeros(m.getColumns(),m.getColumns());
		
		DoubleMatrix top = DoubleMatrix.concatHorizontally(svdResults[1],z);
		DoubleMatrix bottom = DoubleMatrix.concatHorizontally(z.transpose(),z2);
		DoubleMatrix comb = DoubleMatrix.concatVertically(top, bottom);
		
		DoubleMatrix mRA = DoubleMatrix.concatVertically(m, rA);
		DoubleMatrix nRB = DoubleMatrix.concatVertically(n, rB);
	
		DoubleMatrix mRANRB = mRA.mmul(nRB.transpose());
		DoubleMatrix k = comb.add(mRANRB);
		
		DoubleMatrix[] newBases = computeTruncatedSVD(k, Double.MIN_VALUE, maxDim);
				
		DoubleMatrix uP = (DoubleMatrix.concatHorizontally(svdResults[0].transpose(), p)).mmul(newBases[0].transpose());
		DoubleMatrix vP = (DoubleMatrix.concatHorizontally(biggerV,q)).mmul(newBases[2]);
		
		DoubleMatrix[] updatedSVDResults = new DoubleMatrix[3];
		
		updatedSVDResults[0] = uP.transpose();
		updatedSVDResults[1] = newBases[1];
		updatedSVDResults[2] = vP;
		
		double[] singVals = new double[updatedSVDResults[1].getRows()];
		for(int i = 0; i < updatedSVDResults[1].getRows(); i++)
			singVals[i] = updatedSVDResults[1].get(i,i);
				
		return updatedSVDResults;
	}
	
	protected void constructAOMatrices()
	{
		
		List<ActionObservation> currentSequence = new ArrayList<ActionObservation>();
		int resetCount = 0;
		//simulating run for specified number of iterations
		while(resetCount < trainData.getNumberOfRunsInBatch())
		{
			appendNextActionObservation(currentSequence);

			setAONullHistories(currentSequence);

			parseAndAddAOCounts(currentSequence);

			//checking if reset performed
			if(trainData.resetPerformed())
			{
				resetCount++;
				currentSequence = new ArrayList<ActionObservation>();
				//incrementing reset count
			}
		}
	}
	
	/**
	 * Helper method parses sequence into tests and histories and add counts
	 * 
	 * @param currentSequence The current sequence of action-observation pairs
	 */
	protected void parseAndAddAOCounts(List<ActionObservation> currentSequence)
	{
		int hi;
		List<ActionObservation> currentHistory = new ArrayList<ActionObservation>();

		//looping through current sequence and parsing into possible sets of histories and tests
		for(int j = 0; j < currentSequence.size() && j < maxHistLen; j++)
		{
			//making history first part of current sequence
			currentHistory = currentSequence.subList(0, j+1);
			hi = histories.indexOf(currentHistory)+1;

			if(j+2 < currentSequence.size())
			{
				ActionObservation actOb = currentSequence.get(j+1);
				int ti = tests.indexOf(currentSequence.subList(j+2, currentSequence.size()));
				incrementAOMat(actOb, ti,hi);
			}
		}
	}
	
	protected void setAONullHistories(List<ActionObservation> currentSequence)
	{
		List<ActionObservation> seq = currentSequence.subList(1, currentSequence.size());
		ActionObservation actOb = currentSequence.get(0);
		
		incrementAOMat(actOb, tests.indexOf(seq), 0);
	}
	
	protected void incrementAOMat(ActionObservation actOb, int testIndex, int histIndex)
	{
		if(testIndex == -1 || testIndex >= svdResults[0].getColumns())
			return;
		
		DoubleMatrix aoMat = aoMats.get(actOb);
		DoubleMatrix colToAdd = svdResults[0].getColumn(testIndex);
		
		for(int rowIndex = 0; rowIndex < colToAdd.getRows(); rowIndex++)
		{
			double currEntry = colToAdd.get(rowIndex, 0);
			DoubleMatrix currRowToAdd = pseudoInverse.getRow(histIndex);
			currRowToAdd.muli(currEntry);
			aoMat.putRow(rowIndex, aoMat.getRow(rowIndex).add(currRowToAdd));
		}
	}
	
	protected DoubleMatrix[] computeTruncatedSVD(DoubleMatrix mat, Double singValTol, int maxSize)
	{
		DoubleMatrix[] svdResult = Singular.sparseSVD(mat);
		
		notifyPSRObservers(svdResult[1].toArray());
		
		int singIter;
		for(singIter = 0; singIter < svdResult[1].getRows(); singIter++)
		{
			if(svdResult[1].get(singIter,0) < singValTol)
				break;
		}
		
		int numToKeep = Math.min(singIter, maxSize);
		maxDim = numToKeep;
				
		DoubleMatrix u = new DoubleMatrix(mat.getRows(),numToKeep);
		u = svdResult[0].getRange(0,svdResult[0].getRows(), 0, numToKeep);
		DoubleMatrix s = DoubleMatrix.diag(svdResult[1].getRange(0, numToKeep, 0, 1));
		DoubleMatrix v = new DoubleMatrix(mat.getColumns(), numToKeep);
		v = svdResult[2].getRange(0, svdResult[2].getRows(), 0, numToKeep);
		
		DoubleMatrix[] truncatedSVDResult = {u.transpose(),s,v};
		return truncatedSVDResult;
	}

	protected void estimateHistoryAndTHMatrices()
	{
		List<ActionObservation> currentSequence = new ArrayList<ActionObservation>();

		resetCount = 0;

		//simulating run for specified number of iterations
		while(resetCount < trainData.getNumberOfRunsInBatch())
		{
			appendNextActionObservation(currentSequence);

			incrementHistory(currentSequence);

			setTHNullHistories(currentSequence);

			parseAndAddTHCounts(currentSequence);

			//checking if reset performed
			if(trainData.resetPerformed())
			{
				currentSequence = new ArrayList<ActionObservation>();
				//adding one to prob of null history
				hist.put(0, 0, hist.get(0, 0)+1);
				//incrementing reset count
				resetCount++;
			}
		}
		//normalize(resetCount);
	}
	
	/**
	 * Helper method gets next action-observation pair
	 */
	protected void appendNextActionObservation(List<ActionObservation> currentSequence)
	{
		//getting next ActionObservation and adding to current sequence
		ActionObservation currentActionObservation = trainData.getNextActionObservation();
		currentSequence.add(currentActionObservation);
	}

	/**
	 * Helper method increments history count for this sequence
	 * 
	 * @param currentSequence The current sequence of action-observation pairs
	 */
	protected void incrementHistory(List<ActionObservation> currentSequence)
	{
		//incrementing history count for this sequence
		int hi = histories.indexOf(currentSequence)+1;
		if(hi != 0) hist.put(hi,0, hist.get(hi,0)+1);
	}

	/**
	 * Helper method sets the null history columns of observable matrices
	 * 
	 * @param currentSequence The current sequence of action-observation pairs.  
	 */
	protected void setTHNullHistories(List<ActionObservation>  currentSequence)
	{		
			int ti = tests.indexOf(currentSequence);
			addTHCount(ti, 0);
	}
	
	/**
	 * Helper method parses sequence into tests and histories and add counts
	 * 
	 * @param currentSequence The current sequence of action-observation pairs
	 */
	protected void parseAndAddTHCounts(List<ActionObservation> currentSequence)
	{
		int hi;
		List<ActionObservation> currentHistory = new ArrayList<ActionObservation>();

		//looping through current sequence and parsing into possible sets of histories and tests
		for(int j = 0; j < currentSequence.size() && j < maxHistLen; j++)
		{
			//making history first part of current sequence
			currentHistory = currentSequence.subList(0, j+1);
			hi = histories.indexOf(currentHistory)+1;

			if(j+1 < currentSequence.size())
			{
				int ti = tests.indexOf(currentSequence.subList(j+1, currentSequence.size()));
				addTHCount(ti, hi);
			}
		}
	}

	/**
	 * Adds a th count
	 * 
	 * @param ti test index
	 * @param hi history index
	 */
	protected void addTHCount(int ti, int hi)
	{
		if(ti != -1 && ti < th.getRows() && hi < th.getColumns())
		{
			th.put(ti,hi, th.get(ti,hi)+1);
		}
	}
	

}
