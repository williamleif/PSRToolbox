package pomdp.algorithms.pointbased;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Vector;
import java.util.Map.Entry;

import pomdp.algorithms.ValueIteration;
import pomdp.algorithms.backup.BackupOrdering;
import pomdp.algorithms.backup.BackupOrderingFactory;
import pomdp.algorithms.beliefcollection.BeliefCollection;
import pomdp.algorithms.beliefcollection.BeliefCollectionFactory;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.sort.QuickSort;
import pomdp.utilities.sort.SortStrategy;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class GenericValueIteration extends ValueIteration {

	BeliefCollection collection;
	BackupOrdering ordering;
	int numBeliefsPerStep;
	int numBackupIterations;
	boolean useBlindPolicy;
	boolean m_bUseFIB;

	public GenericValueIteration(POMDP pomdp, String collectionName, String backupOrderingName, int numBeliefs, int backupIterations, boolean useBlindPolicy, boolean bAllowDuplicates, boolean bReversedBackupOrder, boolean bUseFIB){

		super(pomdp);
		collection = BeliefCollectionFactory.getBeliefCollectionAlgorithm(collectionName, this, bAllowDuplicates, bUseFIB);
		ordering = BackupOrderingFactory.getBackupOrderingAlgorithm(backupOrderingName, this, bReversedBackupOrder);
		numBeliefsPerStep = numBeliefs;
		numBackupIterations = backupIterations;
		this.useBlindPolicy = useBlindPolicy;
		m_bUseFIB = bUseFIB;
	}


	public void valueIteration(int cIterations, double dEpsilon, double dTargetValue, int maxRunningTime, int numEvaluations){

		try{
			m_pPOMDP.getBeliefStateFactory().cacheBeliefStates(false);
			
			Logger.getInstance().logln("\n\nStarting POMDP solver\n" + getName() + "\n");

			String sModelName = m_pPOMDP.getName();
			PrintStream ps = new PrintStream( new FileOutputStream( "logs/Basic/" + sModelName + "." + 
																					collection + "." + 
																					ordering + "." + 
																					numBeliefsPerStep + "." +
																					numBackupIterations + "." +
																					ordering.isReversed() + "." +
																					numBackupIterations + "." +
																					collection.allowDuplicates() + "." + 
																					(m_bUseFIB ? "FIB" : "QMDP") + "." +
																					(useBlindPolicy ? "BlindPolicy" : "MinPolicy") + "." +
																					"txt"
																					, true ) );
			ps.println( collection + "\t" + ordering );
			int currentEvaluation = 1;

			// with numEvaluations evaluations, we find out how much time alloted per evaluation
			double timePerEval = (double)maxRunningTime / numEvaluations;

			boolean bDone = false;

			/* keep track of overall time */
			long lStartTime = JProf.getCurrentThreadCpuTimeSafe();
			long lCurrentTime;
			Runtime rtRuntime = Runtime.getRuntime();

			double dDelta = 1.0;

			long lCPUTimeBefore, lCPUTimeAfter;

			/* store the amount of time required for each segment of the algorithm */
			long initalBeliefTime = 0;
			long collectionTime = 0;
			long updateTime = 0;
			double totalTimeInSeconds = 0;
			double previousTotalTime = 0;
			double dPreviousReward = m_pPOMDP.computeAverageDiscountedReward(500, 150, getValueFunction());

			Vector<BeliefState> newBeliefPoints = new Vector<BeliefState>();

			LinearValueFunctionApproximation oldVF = getValueFunction();

			/* get the initial belief set from the collection algorithm */
			lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();

			if (useBlindPolicy)
				initValueFunctionUsingBlindPolicy();
			else
				initValueFunctionToMin();

			Vector<BeliefState> beliefPoints = collection.initialBelief();
			lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			initalBeliefTime += (lCPUTimeAfter - lCPUTimeBefore);

			for (int i = 0; i < cIterations && !bDone; i++){

				System.gc();
				totalTimeInSeconds = (initalBeliefTime + collectionTime + updateTime)/1000000000.0;

				/* expand the belief set, and gather a set of new belief points */
				Logger.getInstance().logln("Expanding belief space:");
				lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();

				if (numBeliefsPerStep <= 0)
					newBeliefPoints = collection.expand(beliefPoints);
				else
					newBeliefPoints = collection.expand(numBeliefsPerStep, beliefPoints);
				
				lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
				collectionTime += (lCPUTimeAfter - lCPUTimeBefore);
				Logger.getInstance().logln("Finished expansion, " + newBeliefPoints.size() + " new belief points\n");


				Logger.getInstance().logln("Improving value function:");
				lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
				ordering.improveValueFunction(oldVF, beliefPoints, newBeliefPoints, numBackupIterations);
				lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
				updateTime += (lCPUTimeAfter - lCPUTimeBefore);
				Logger.getInstance().logln("Finished improving");

				// add the new beliefs to the current set and remove duplicates 
				beliefPoints.addAll(newBeliefPoints);
				/*removing duplicates here is problematic because we can't control it using the flag.
				 * On the other hand, if we move it elsewhere, we have to do so BEFORE the backup 
				 * and newest points backup will not have b_0 and will behave horribly.
				 * Currently just commenting this out.
				 
				Collection<BeliefState> cleaned = new LinkedHashSet<BeliefState>(beliefPoints);
				beliefPoints.clear();
				beliefPoints.addAll(cleaned);*/

				totalTimeInSeconds = (initalBeliefTime + collectionTime + updateTime)/1000000000.0;


				/* need to log the quality */
				Logger.getInstance().logln("Current Time: " + totalTimeInSeconds);
				double currentTarget = currentEvaluation*timePerEval;
				if (totalTimeInSeconds > currentTarget)
				{
					// we will use the value function from the previous iteration for testing. This way, we
					//will always be pessimistic in the quality of the solution 

					currentEvaluation = (int)(totalTimeInSeconds / timePerEval) + 1;
					Logger.getInstance().logln("Computing Solution Quality:");
					double dDiscountedReward = m_pPOMDP.computeAverageDiscountedReward(500, 150, getValueFunction());
					Logger.getInstance().log("POMDPSolver", 0, "main", "ADR = " + dDiscountedReward + ", time = " + totalTimeInSeconds);
					// this is to capture all possible time intervals we may have missed 

					if (totalTimeInSeconds > currentTarget){
						while ((currentTarget < totalTimeInSeconds) && (currentTarget <= maxRunningTime))
						{
							double dInterpolatedValue = computeLinearInterpolation( totalTimeInSeconds, previousTotalTime, dDiscountedReward, dPreviousReward, currentTarget );
							ps.println(currentTarget + "\t" + previousTotalTime + "\t" + dInterpolatedValue + "\t" + dPreviousReward);
							
							//System.out.println(currentTarget + "\t" + previousTotalTime + "\t" + dInterpolatedValue + "\t" + dPreviousReward);
							
							ps.flush();

							currentTarget += timePerEval;

						}
					}


					dPreviousReward = dDiscountedReward;
				}


				if (totalTimeInSeconds > maxRunningTime)
					bDone = true;


				/* store the previous VF */
				oldVF = getValueFunction();
				previousTotalTime = totalTimeInSeconds;

				lCurrentTime = JProf.getCurrentThreadCpuTimeSafe();

				Logger.getInstance().logln( "Iteration " + i +
						" |Vn| = " + m_vValueFunction.size() +
						" |B| = " + beliefPoints.size() +
						" Delta = " + round( dDelta, 4 ) +
						" Time " + ( lCurrentTime - lStartTime ) / 1000000 +
						" Initial Belief Time " + initalBeliefTime / 1000000 +
						" Belief Collection Time " + collectionTime / 1000000 +
						" Update Time " + updateTime / 1000000 +
						" #backups: " + m_cBackups +
						" #dot product: " + AlphaVector.dotProductCount() +
						" |BS| " + m_pPOMDP.getBeliefStateFactory().getBeliefStateCount() +
						" memory: " +
						" total: " + rtRuntime.totalMemory() / 1000000 +
						" free: " + rtRuntime.freeMemory() / 1000000 +
						" max: " + rtRuntime.maxMemory() / 1000000 +
				"" );
			}
			Logger.getInstance().logln( "Finished " + getName() + " - Model : " + m_pPOMDP.getName() +  " - time : " + totalTimeInSeconds + " |BS| = " + beliefPoints.size() +
					" |V| = " + m_vValueFunction.size() + " backups = " + m_cBackups + " GComputations = " + AlphaVector.getGComputationsCount()+

					" Initial Belief Time " + initalBeliefTime / 1000000 +
					" Belief Collection Time " + collectionTime / 1000000 +
					" Update Time " + updateTime / 1000000


			);
			ps.println();
			ps.close();
		}
		catch(IOException e){
			Logger.getInstance().logError( "GenericVI", "valueIteration", e.getMessage() );
			e.printStackTrace();
		}
	}

	private double computeLinearInterpolation(double dX1, double dX2, double dY1, double dY2, double dNewX){
		
		double dB = 0.0;
		if( dX1 != dX2 )
			dB = ( dY1 - dY2 ) / (dX1 - dX2 );
		double dA = dY1 - dX1 * dB;
		double dNewY = dA + dB * dNewX;
		return dNewY;
	}

	public String getName(){
		return "Collection Algorithm: " + collection + "  Ordering Algorithm:  " + ordering;
	}
}
