package cpsr.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jblas.DoubleMatrix;
import org.jblas.Solve;

import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.ActionObservation;
import cpsr.model.CPSR.ProjType;
import cpsr.model.components.Minf;
import cpsr.model.components.PredictionVector;

public class MemEffCPSR extends TPSR 
{

	protected static long SEED = 10;

	public static final int MAX_CACHE_SIZE = 100000;

	MaxSizeHashMap<Integer, DoubleMatrix> randVecCache, colToAddCache, rowToAddCache;

	protected boolean randStart;

	protected ProjType projType;

	protected int projDim;


	public MemEffCPSR(TrainingDataSet trainData, double minSingularVal, int maxSVDDim, int projDim, ProjType projType, boolean randStart)
	{

		this(trainData, minSingularVal, maxSVDDim, projDim, CPSR.DEF_MAX_HIST, projType,  randStart);
	}

	public MemEffCPSR(TrainingDataSet trainData, double minSingularVal, int maxSVDDim, int projDim, int maxHistLen, ProjType projType, boolean randStart)
	{
		super(trainData, minSingularVal, maxSVDDim, maxHistLen);
		this.projType = projType;
		this.projDim = projDim;
		this.randVecCache = new MaxSizeHashMap<Integer, DoubleMatrix>(1000, 1000);
		
	}


	@Override
	protected void performBuild() 
	{
		this.colToAddCache = new MaxSizeHashMap<Integer, DoubleMatrix>(100000, 10000);
		this.rowToAddCache = new MaxSizeHashMap<Integer, DoubleMatrix>(100000, 10000);
		//initializing observable matrices
		hist = new DoubleMatrix(projDim+1, 1);
		th = new DoubleMatrix(projDim, projDim+1);
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
		pv = new PredictionVector(svdResults[1].mmul(svdResults[2].transpose()).getColumn(0));
	}

	protected void performUpdate()
	{
		this.colToAddCache = new MaxSizeHashMap<Integer, DoubleMatrix>(100000, 10000);
		this.rowToAddCache = new MaxSizeHashMap<Integer, DoubleMatrix>(100000, 10000);

		//	hPhi.divi(((double)histories.size()+1)/((double)hPhi.getRows()));


		th = new DoubleMatrix(th.getRows(), th.getColumns());

		estimateHistoryAndTHMatrices();
		//		th.muli(1.0/((double)resetCount));
		//		hist.muli(1.0/((double)resetCount));

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
		DoubleMatrix one = DoubleMatrix.zeros(histories.size()+1, 1);
		one.put(0,0, 1.0);
		if(randStart)
		{
			for(int i = 0; i < one.getRows(); i++)
				one.put(i,0,1.0);
		}

		mInf = new Minf(((hist.transpose()).mmul(pseudoInverse)).transpose());
		pv = new PredictionVector(svdResults[1].mmul(svdResults[2].transpose()).getColumn(0));
	}


	protected void incrementAOMat(ActionObservation actOb, int testIndex, int histIndex)
	{
		if(testIndex == -1)
			return;

		DoubleMatrix aoMat = aoMats.get(actOb);

		
		DoubleMatrix colToAdd;
		
		if(colToAddCache.containsKey(testIndex))
		{
			colToAdd = colToAddCache.get(testIndex);
		}
		else
		{
			colToAdd = svdResults[0].mmul(getRandomVector(projDim, projType, testIndex));
			colToAddCache.put(testIndex, colToAdd);
		}
		

		for(int rowIndex = 0; rowIndex < colToAdd.getRows(); rowIndex++)
		{
			double currEntry = colToAdd.get(rowIndex, 0);

			DoubleMatrix randHistVector;
			if(histIndex == 0)
			{
				randHistVector = DoubleMatrix.zeros(projDim+1,1);
				randHistVector.put(0, 0,1);
			}
			else
			{
				randHistVector = getRandomVector(projDim,projType,tests.size()+histIndex);
				randHistVector = DoubleMatrix.concatVertically(DoubleMatrix.zeros(1), randHistVector);
			}

			DoubleMatrix currRowToAdd;
			
			if(rowToAddCache.containsKey(histIndex))
			{
				currRowToAdd = rowToAddCache.get(histIndex);
			}
			else
			{
				currRowToAdd = (randHistVector.transpose()).mmul(pseudoInverse);
				rowToAddCache.put(histIndex, currRowToAdd);
			}
		
			currRowToAdd.muli(currEntry);
			aoMat.putRow(rowIndex, aoMat.getRow(rowIndex).add(currRowToAdd));
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
		if(ti != -1 && hi != -1)
		{

			DoubleMatrix colToAdd = getRandomVector(projDim,projType, ti);

			for(int rowIndex = 0; rowIndex < colToAdd.getRows(); rowIndex++)
			{
				double currEntry = colToAdd.get(rowIndex, 0);

				DoubleMatrix currRowToAdd;
				if(hi == 0)
				{
					currRowToAdd = DoubleMatrix.zeros(projDim+1,1);
					currRowToAdd.put(0,0,1);
				}
				else
				{
					currRowToAdd = getRandomVector(projDim,projType,tests.size()+hi);
					currRowToAdd = DoubleMatrix.concatVertically(DoubleMatrix.zeros(1), currRowToAdd);
				}
				currRowToAdd = currRowToAdd.transpose();

				currRowToAdd.muli(currEntry);
				th.putRow(rowIndex, th.getRow(rowIndex).add(currRowToAdd));
			}
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
		int hi = histories.indexOf(currentSequence)+1;

		DoubleMatrix randHistVector;

		randHistVector = getRandomVector(projDim,projType,tests.size()+hi);
		randHistVector = DoubleMatrix.concatVertically(DoubleMatrix.zeros(1), randHistVector);


		if(hi != 0) hist.putColumn(0, randHistVector.add(hist.getColumn(0)));
	}


	@SuppressWarnings("incomplete-switch")
	protected DoubleMatrix getRandomVector(int projDim, ProjType projType, int index)
	{

		if(randVecCache.containsKey(index))
		{
			return randVecCache.get(index);
		}
		else
		{

			DoubleMatrix randVec = new DoubleMatrix(projDim,1);

			Random rand = new Random(index);

			switch(projType)
			{
			case Spherical:
				//setting entries to random numbers drawn from gaussian distrubution.
				for(int i = 0; i < randVec.getRows(); i++)
				{
					randVec.put(i,0,rand.nextGaussian()/((double)projDim));
				}
				break;
			case Bernoulli:
				//setting entries to random numbers drawn from gaussian distrubution.
				for(int i = 0; i < randVec.getRows(); i++)
				{
					if(rand.nextBoolean())
					{
						randVec.put(i, 0,1.0/((double)projDim));
					}
					else
					{
						randVec.put(i, 0, -1.0/((double)projDim));
					}
				}
				break;
			case ModifiedBernoulli:
				//setting entries to random numbers drawn from gaussian distrubution.
				for(int i = 0; i < randVec.getRows(); i++)
				{
					Double randDub = rand.nextDouble();
					if(randDub < 1.0/6.0)
					{
						randVec.put(i,0, 1.0);
					}
					else if(randDub > 5.0/6.0)
					{
						randVec.put(i,0,-1.0);
					}
					else
					{
						randVec.put(i,0,0);
					}
				}
			}
			
			randVecCache.put(index, randVec);
			
			return randVec;
		}
	}

	private static class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V>
	{
		private final int maxSize;

		public MaxSizeHashMap(int maxSize, int initCapacity) {
			super(initCapacity, 0.75f, true);
			this.maxSize = maxSize;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
			return size() > maxSize;
		}
	}


}


