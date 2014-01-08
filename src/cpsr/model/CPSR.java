package cpsr.model;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.jblas.DoubleMatrix;
import org.jblas.Singular;
import org.jblas.Solve;

import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.ActionObservation;
import cpsr.model.components.Minf;
import cpsr.model.components.PredictionVector;

public class CPSR extends TPSR 
{

	private static final long serialVersionUID = -7524663933519008198L;
	
	public static final int DEF_MAX_HIST = 10000;

	protected static long SEED = 10;
	
	protected boolean randStart, histCompress;

	protected DoubleMatrix tPhi, hPhi,e, aoColAddMat, aoRowAddMat;

	protected ProjType projType;

	protected int projDim;
	
	public enum ProjType {Spherical,Bernoulli,ModifiedBernoulli, Eye};

	public CPSR(TrainingDataSet trainData, double minSingularVal, int maxSVDDim, int projDim, ProjType projType, boolean histCompress, boolean randStart)
	{
	
		this(trainData, minSingularVal, maxSVDDim, projDim, DEF_MAX_HIST, projType, histCompress, randStart);
	}

	public CPSR(TrainingDataSet trainData, double minSingularVal, int maxSVDDim, int projDim, int maxHistLen, ProjType projType, boolean histCompress, boolean randStart)
	{
		super(trainData, minSingularVal, maxSVDDim, maxHistLen);
		this.projType = projType;
		this.histCompress = histCompress;
		this.projDim = projDim;

		tPhi = constructRandomMatrix(projType, projDim, tests.size(), projDim);
		if(!histCompress)
		{
			hPhi = constructRandomMatrix(ProjType.Eye,histories.size()+1,histories.size()+1, 1.0);
		}
		else
		{
			hPhi = constructRandomMatrix(projType,histories.size()+1,projDim, projDim);
		}

		DoubleMatrix one = DoubleMatrix.zeros(histories.size()+1, 1);
		this.randStart = randStart;

		one.put(0,0, 1.0);
		if(randStart)
		{
			for(int i = 0; i < one.getRows(); i++)
				one.put(i,0,1.0);
		}

		if(histCompress)
		{
			DoubleMatrix[] hPhiSVD = Singular.sparseSVD(hPhi);
			DoubleMatrix sigma = DoubleMatrix.diag(hPhiSVD[1]);


			e = hPhiSVD[2].mmul(Solve.pinv(sigma)).mmul(hPhiSVD[0].transpose());
			
			e = e.mmul(one);
		}
		else
		{
			e = one;
		}
		
		
	}

	protected DoubleMatrix constructRandomMatrix(ProjType projType, int rows, int cols, double scale)
	{
		DoubleMatrix randMat = DoubleMatrix.zeros(rows, cols);

		Random rand = new Random(SEED);

		switch(projType)
		{
		case Spherical:
			//setting entries to random numbers drawn from gaussian distrubution.
			for(int i = 0; i < randMat.getRows(); i++)
			{
				for(int j = 0; j < randMat.getColumns(); j++)
				{
					randMat.put(i,j, rand.nextGaussian()/(double)scale);
				}
			}
			break;
		case Bernoulli:
			//setting entries to random numbers drawn from gaussian distrubution.
			for(int i = 0; i < randMat.getRows(); i++)
			{
				for(int j = 0; j < randMat.getColumns(); j++)
				{
					if(rand.nextBoolean())
					{
						randMat.put(i,j, 1.0);
					}
					else
					{
						randMat.put(i,j,-1.0);
					}
				}
			}
			break;
		case ModifiedBernoulli:
			//setting entries to random numbers drawn from gaussian distrubution.
			for(int i = 0; i < randMat.getRows(); i++)
			{
				for(int j = 0; j < randMat.getColumns(); j++)
				{
					Double randDub = rand.nextDouble();
					if(randDub < 1.0/6.0)
					{
						randMat.put(i,j, 1.0);
					}
					else if(randDub > 5.0/6.0)
					{
						randMat.put(i,j,-1.0);
					}
					else
					{
						randMat.put(i,j,0);
					}
				}
			}
			System.out.println("here");
		case Eye:
			randMat =  DoubleMatrix.eye(rows);
		}
		return randMat;

	}

	@Override
	protected void performBuild() 
	{
		//initializing observable matrices
		hist = new DoubleMatrix(hPhi.getColumns(), 1);
		th = new DoubleMatrix(tPhi.getRows(), hPhi.getColumns());
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
		aoColAddMat = (svdResults[0].mmul(tPhi));
		aoRowAddMat = (hPhi.mmul(pseudoInverse));
		constructAOMatrices();
//		for(ActionObservation actOb : trainData.getValidActionObservationSet())
//		{
//			aoMats.put(actOb, aoMats.get(actOb).muli(1.0/((double)resetCount)));
//		}
		resetCount=0;
	
		mInf = new Minf(((hist.transpose()).mmul(pseudoInverse)).transpose());
		pv = new PredictionVector(svdResults[0].mmul(th).mmul(e));
	}

	protected void performUpdate()
	{

		DoubleMatrix newPhiCols = constructRandomMatrix(projType,tPhi.getRows(), tests.size()-tPhi.getColumns(), projDim);

	//	tPhi.divi(((double)tests.size())/((double)(tPhi.getColumns())));
		tPhi = DoubleMatrix.concatHorizontally(tPhi, newPhiCols);
		
		if(!histCompress)
		{
			hPhi = constructRandomMatrix(ProjType.Eye,histories.size()+1,histories.size()+1, 1.0);
			hist = DoubleMatrix.concatVertically(hist, 
					DoubleMatrix.zeros(histories.size()+1-hist.getRows(),1));
		}
		else
		{
			DoubleMatrix newHPhiRows;
			newHPhiRows = constructRandomMatrix(projType,histories.size()+1-hPhi.getRows(), hPhi.getColumns(), projDim);
			hPhi = DoubleMatrix.concatVertically(hPhi, newHPhiRows);
		}

	//	hPhi.divi(((double)histories.size()+1)/((double)hPhi.getRows()));


		th = new DoubleMatrix(th.getRows(), hPhi.getColumns());
		
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
		aoColAddMat = (svdResults[0].mmul(tPhi));
		
		if(histCompress)
		{
			aoRowAddMat = (hPhi.mmul(pseudoInverse));
		}
		else
		{
			aoRowAddMat = pseudoInverse;
		}
		
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

		if(histCompress)
		{
			DoubleMatrix[] hPhiSVD = Singular.sparseSVD(hPhi);
			DoubleMatrix sigma = DoubleMatrix.diag(hPhiSVD[1]);

			e = hPhiSVD[2].mmul(Solve.pinv(sigma)).mmul(hPhiSVD[0].transpose());
		
			e = e.mmul(one);
		}
		else
		{
			e = one;
		}
		
		mInf = new Minf(((hist.transpose()).mmul(pseudoInverse)).transpose());
		pv = new PredictionVector(svdResults[1].mmul(svdResults[2].transpose()).mmul(e));

	}


	protected void incrementAOMat(ActionObservation actOb, int testIndex, int histIndex)
	{
		if(testIndex == -1)
			return;

		DoubleMatrix aoMat = aoMats.get(actOb);

		DoubleMatrix colToAdd = aoColAddMat.getColumn(testIndex);

		for(int rowIndex = 0; rowIndex < colToAdd.getRows(); rowIndex++)
		{
			double currEntry = colToAdd.get(rowIndex, 0);
			DoubleMatrix currRowToAdd = aoRowAddMat.getRow(histIndex);
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
			DoubleMatrix colToAdd = tPhi.getColumn(ti);

			for(int rowIndex = 0; rowIndex < colToAdd.getRows(); rowIndex++)
			{
				double currEntry = colToAdd.get(rowIndex, 0);
				DoubleMatrix currRowToAdd = hPhi.getRow(hi);
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
		
		if(hi != 0) hist.putColumn(0, hPhi.getRow(hi).transpose().add(hist.getColumn(0)));
	}




}
