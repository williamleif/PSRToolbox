package cpsr.model;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.jblas.DoubleMatrix;
import org.jblas.Solve;

import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.ActionObservation;
import cpsr.model.components.Minf;
import cpsr.model.components.PredictionVector;

public class HashedCPSR extends TPSR {

	protected int hashedDim, histSeed, testSeed;
	protected boolean histCompress;
	
	public HashedCPSR(TrainingDataSet trainData, double minSingularVal,
			int maxDim, int maxHistLen, int hashedDim, boolean histCompress) 
	{
		super(trainData, minSingularVal, maxDim, maxHistLen);
		this.hashedDim = hashedDim;
		this.histCompress = histCompress;
		
		Random rando = new Random();
		histSeed = rando.nextInt();
		testSeed = rando.nextInt();
	}

	public HashedCPSR(TrainingDataSet trainData, double minSingularVal,
			int maxDim, int hashedDim, boolean histCompress) {
		super(trainData, minSingularVal, maxDim);
		this.hashedDim = hashedDim;
		this.histCompress = histCompress;
		
		Random rando = new Random();
		histSeed = rando.nextInt();
		testSeed = rando.nextInt();
	}
	
	@Override
	protected void performBuild() 
	{
		//initializing observable matrices
		if(histCompress)
		{
			hist = new DoubleMatrix(hashedDim+1, 1);
			th = new DoubleMatrix(hashedDim, hashedDim+1);
		}
		else
		{
			hist = new DoubleMatrix(histories.size()+1,1);
			th = new DoubleMatrix(hashedDim, histories.size()+1);
		}
		
		estimateHistoryAndTHMatrices();
//		th.muli(1.0/((double)resetCount));
//		hist.muli(1.0/((double)resetCount));
		
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
//			aoMats.put(actOb, aoMats.get(actOb).muli(1.0/((double)resetCount)));
//		}
		resetCount=0;
	
		mInf = new Minf(((hist.transpose()).mmul(pseudoInverse)).transpose());
		pv = new PredictionVector(svdResults[0].mmul(th.getColumn(0)));
	}
	
	protected void performUpdate()
	{
		th = DoubleMatrix.zeros(th.getRows(), th.getColumns());
	
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
	
	
	
	protected void incrementAOMat(ActionObservation actOb, int testIndex, int histIndex)
	{
		if(testIndex == -1)
			return;
		
		int hashedTInd = getHash(testIndex);
		int hashedHIndex = 0;
		
		if(histCompress)
		{
			hashedHIndex = histIndex == 0 ? 0 : getHash(histIndex)+1;
		}
		else
		{
			hashedHIndex = histIndex;
		}	
		
		DoubleMatrix aoMat = aoMats.get(actOb);
		DoubleMatrix colToAdd = svdResults[0].getColumn(hashedTInd);
		
		for(int rowIndex = 0; rowIndex < colToAdd.getRows(); rowIndex++)
		{
			double currEntry = colToAdd.get(rowIndex, 0);
			DoubleMatrix currRowToAdd = pseudoInverse.getRow(hashedHIndex);
			currRowToAdd.muli(currEntry);
			aoMat.putRow(rowIndex, aoMat.getRow(rowIndex).add(currRowToAdd));
		}
	}
	
	/**
	 * Helper method increments history count for this sequence
	 * 
	 * @param currentSequence The current sequence of action-observation pairs
	 */
	protected void incrementHistory(List<ActionObservation> currentSequence)
	{
		//incrementing history count for this sequence
		int histIndex = histories.indexOf(currentSequence)+1;
		
		int hashedHIndex = 0;
		
		if(histCompress)
		{
			hashedHIndex = histIndex == 0 ? 0 : getHash(histIndex)+1;
		}
		else
		{
			hashedHIndex = histIndex;
		}
				
		if(histIndex != 0) hist.put(hashedHIndex,0, hist.get(hashedHIndex,0)+1);
	}

	/**
	 * Adds a th count
	 * 
	 * @param ti test index
	 * @param hi history index
	 */
	protected void addTHCount(int testIndex, int histIndex)
	{
		if(testIndex == -1)
			return;
		
		int hashedTInd = getHash(testIndex);
		int hashedHIndex = 0;
		
		if(histCompress)
		{
			hashedHIndex = histIndex == 0 ? 0 : getHash(histIndex)+1;
		}
		else
		{
			hashedHIndex = histIndex;
		}
		
		th.put(hashedTInd, hashedHIndex, th.get(hashedTInd, hashedHIndex)+1);
	}
	
	private int getHash(int val)
	{
		int intResult = MurmurHash3.murmurhash3x8632(ByteBuffer.allocate(4).putInt(val).array(), 0, 4, histSeed);
		long longResult = intResult & 0x00000000ffffffffL;
		return (int)(longResult % hashedDim);
	}
	
	

}
