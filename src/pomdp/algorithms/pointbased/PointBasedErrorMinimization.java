package pomdp.algorithms.pointbased;

import java.util.Iterator;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.valuefunction.LinearValueFunctionApproximation;



public class PointBasedErrorMinimization extends ValueIteration {

	protected boolean m_bSingleValueFunction = false;
	
	public PointBasedErrorMinimization(POMDP pomdp) {
		super(pomdp);
	}


	/***
	 * valueIteration
	 * @param cIterations number of iterations of belief point expansions / backups
	 * @param dEpsilon epsilon for verifying if the value function has changed
	 * @param dTargetValue target filted value for averaged discounted reward
	 */
	
	public void valueIteration(int cIterations, double dEpsilon, double dTargetValue, int maxRunningTime, int numEvaluations)
	
	{
		
		/* number of iterations of value function updates */
		int cInternalIterations = 10;
		
		double dDelta = 1.0;
		double dMinDelta = 0.01;
		
		int iIteration = 0;
		int iInternalIteration = 0;
		int cBeliefPoints = 0;
		
		/* simulated and filtered Averaged Discounted Reward */
		Pair<Double, Double> pComputedADRs = new Pair<Double, Double>(new Double( 0.0 ), new Double( 0.0 ));
		boolean bDone = false;
		boolean bDoneInternal = false;
		long lStartTime = System.currentTimeMillis();
		long lCurrentTime = 0;
		Runtime rtRuntime = Runtime.getRuntime();
		

		

		
		m_cElapsedExecutionTime = 0;
		m_cCPUExecutionTime = 0;
		
		long lCPUTimeBefore = 0, lCPUTimeAfter = 0, lCPUTimeTotal = 0;
		
		int cValueFunctionChanges = 0;		
			
		Vector<BeliefState>	vBeliefPoints = new Vector<BeliefState>();
		
		/* initialize the list of belief points with the initial belief state */
		vBeliefPoints.add( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() );
		
		Logger.getInstance().logln( "Begin " + getName() );

		/* iterations of belief point expansions and backups */
		for(iIteration = 0 ; iIteration < cIterations && !bDone ; iIteration++){
			lStartTime = System.currentTimeMillis();
			lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			
			/* first, expand the belief set */
			if(iIteration > 0){
				Logger.getInstance().logln( "Expanding belief space" );
				m_dFilteredADR = 0.0;
				cBeliefPoints = vBeliefPoints.size();
				vBeliefPoints = expand(vBeliefPoints);
				Logger.getInstance().logln("Expanded belief space - |B| = " + vBeliefPoints.size());
				
				/* if we do not increase the size of the belief space we stop */
				if( vBeliefPoints.size() == cBeliefPoints )
					bDone = true;
			}
			
			dDelta = 1.0;
			bDoneInternal = false;
			
			/* iterate through value function updates */
			for(iInternalIteration = 0 ; 
				(iInternalIteration < cInternalIterations) && (dDelta > dMinDelta) && !bDoneInternal ; iInternalIteration++){
				
				cValueFunctionChanges = m_vValueFunction.getChangesCount();
				dDelta = improveValueFunction(vBeliefPoints);
				
				lCurrentTime = System.currentTimeMillis();
				lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
				m_cElapsedExecutionTime += ( lCurrentTime - lStartTime );
				m_cCPUExecutionTime += ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000;
				lCPUTimeTotal += lCPUTimeAfter - lCPUTimeBefore;
				
				/* value function didn't change */
				if(dDelta < dEpsilon && cValueFunctionChanges == m_vValueFunction.getChangesCount()){
					Logger.getInstance().logln( "Value function did not change - iteration " + iIteration + " complete" );
					bDoneInternal = true;
				}
				else
				{
					if( iIteration > 2 ){
						bDone = bDone || checkADRConvergence( m_pPOMDP, dTargetValue, pComputedADRs );
						if(bDone)
							bDoneInternal = true;
					}
					
					rtRuntime.gc();
					Logger.getInstance().logln( "PEMA: Iteration " + iIteration + "," + iInternalIteration +
							" |Vn| = " + m_vValueFunction.size() +
							" |B| = " + vBeliefPoints.size() +
							" Delta = " + round( dDelta, 4 ) +
							" simulated ADR " + ((Number) pComputedADRs.first()).doubleValue() +
							" filtered ADR " + round( ((Number) pComputedADRs.second()).doubleValue(), 3 ) +
							" Time " + ( lCurrentTime - lStartTime ) / 1000 +
							" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
							" CPU total " + lCPUTimeTotal  / 1000000000 +
							" #backups " + m_cBackups + 
							" #dot product " + AlphaVector.dotProductCount() + 
							" |BS| " + m_pPOMDP.getBeliefStateFactory().getBeliefStateCount() +
							" memory: " + 
							" total " + rtRuntime.totalMemory() / 1000000 +
							" free " + rtRuntime.freeMemory() / 1000000 +
							" max " + rtRuntime.maxMemory() / 1000000 +
							"" );
				}
				
				m_cElapsedExecutionTime += ( lCurrentTime - lStartTime );
				
				lStartTime = System.currentTimeMillis();;
				lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			}
		}
		m_cElapsedExecutionTime /= 1000;
		m_cCPUExecutionTime /= 1000;

		Logger.getInstance().logln( "Finished " + getName() + " - time : " + m_cElapsedExecutionTime + " |BS| = " + vBeliefPoints.size() +
				" |V| = " + m_vValueFunction.size() + " backups = " + m_cBackups + " GComputations = " + AlphaVector.getGComputationsCount() );
	}

	
	protected double improveValueFunction(Vector vBeliefPoints){
		LinearValueFunctionApproximation vNextValueFunction = new LinearValueFunctionApproximation(m_dEpsilon, true);
		BeliefState bsCurrent = null, bsMax = null;
		AlphaVector avBackup = null, avNext = null, avCurrentMax = null;
		double dMaxDelta = 1.0, dDelta = 0.0, dBackupValue = 0.0, dValue = 0.0;
		double dMaxOldValue = 0.0, dMaxNewValue = 0.0;
		int iBeliefState = 0;

		boolean bPrevious = m_pPOMDP.getBeliefStateFactory().cacheBeliefStates(false);

		Iterator m_itCurrentIterationPoints = vBeliefPoints.iterator();
		dMaxDelta = 0.0;
		
		/* interate through all belief points */
		while(m_itCurrentIterationPoints.hasNext()){
			bsCurrent= (BeliefState) m_itCurrentIterationPoints.next();
			avCurrentMax = m_vValueFunction.getMaxAlpha( bsCurrent );
			avBackup = backup( bsCurrent );
			
			dBackupValue = avBackup.dotProduct( bsCurrent );
			dValue = avCurrentMax.dotProduct( bsCurrent );
			dDelta = dBackupValue - dValue;
		
			
			if( dDelta > dMaxDelta ){
				dMaxDelta = dDelta;
				bsMax = bsCurrent;
				dMaxOldValue = dValue;
				dMaxNewValue = dBackupValue;
			}
			
			avNext = avBackup;
			if( avNext != null )
				vNextValueFunction.addPrunePointwiseDominated( avNext );
			
			iBeliefState++;
		}
		if( m_bSingleValueFunction ){
			Iterator it = vNextValueFunction.iterator();
			while( it.hasNext() ){
				avNext = (AlphaVector) it.next();
				m_vValueFunction.addPrunePointwiseDominated( avNext );
			}
		}
		else{
			m_vValueFunction.copy( vNextValueFunction );
		}
		
		
		if( !m_itCurrentIterationPoints.hasNext() )
			m_itCurrentIterationPoints = null;
		
		Logger.getInstance().logln( "Max delta over " + bsMax + 
				" from " + round( dMaxOldValue, 3 ) + 
				" to " + round( dMaxNewValue, 3 ) );
		
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bPrevious );
		
		return dMaxDelta;
	}
	
	
	
	
	
	
	public String getName(){
		return "PointBasedErrorMinimization";
	}
	
	
	protected Vector<BeliefState> expand( Vector<BeliefState> vBeliefPoints ){
		
		/* initialize the expanded set with the initial belief set */
		Vector<BeliefState> vExpanded = new Vector<BeliefState>( vBeliefPoints );
		
		Iterator it = vBeliefPoints.iterator();
		BeliefState bsCurrent = null;

		boolean bPrevious = m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
		double maxError = Double.NEGATIVE_INFINITY;
		BeliefState bestBelief = null;
		
		/* iterate through the previous set of belief points */				
		while(it.hasNext()){
			bsCurrent = (BeliefState) it.next();
			
			
			double error = PEMAWeightedErrorBound(vBeliefPoints, bsCurrent);
			if (error > maxError) {
				error = maxError;
				bestBelief = bsCurrent;
			}

		}
		BeliefState bsNext = getPEMADescendant(vBeliefPoints, bestBelief);
		
		if((bsNext != null) && (!vExpanded.contains(bsNext))){
			vExpanded.add(bsNext);
		}

		
		
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bPrevious );
		
		return vExpanded;
	}


	/***
	 * Compute the error bound, Eq. (7) in PEMA paper
	 */
	private double PEMAWeightedErrorBound(Vector<BeliefState> vBeliefPoints, BeliefState bs)
	{
		double maxError = Double.NEGATIVE_INFINITY;
		int action;		
		int observation;
		
		for (action = 0; action < m_pPOMDP.getActionCount(); action++)
		{
			double actionObservationError = 0;
			for (observation = 0; observation < m_pPOMDP.getObservationCount(); observation++)
			{
				double observationProb = m_pPOMDP.O(action, bs, observation);
				BeliefState nextBelief = bs.nextBeliefState(action, observation);
				
				if (observationProb > 0)
				{
					double error = PEMAErrorBound(vBeliefPoints, nextBelief);
					actionObservationError += observationProb * error;		
				}
			}
			
			if (actionObservationError > maxError)
			{
				maxError = actionObservationError;		
			}			
		}
		return maxError;
	}
	
	/***
	 * get the descendant belief that has the largest impact on the error of the current belief.
	 * @param vBeliefPoints is the current set of belief points
	 * @param bs is the current belief
	 */
	private BeliefState getPEMADescendant(Vector<BeliefState> vBeliefPoints, BeliefState bs)
	{
		int action;		
		int observation;
		BeliefState bestBelief = null;
		
		double maxWeightedError = Double.NEGATIVE_INFINITY;
		for (action = 0; action < m_pPOMDP.getActionCount(); action++)
		{
			double actionObservationError = 0;
			
			for (observation = 0; observation < m_pPOMDP.getObservationCount(); observation++)
			{
				double observationProb = m_pPOMDP.O(action, bs, observation);
				
				if (observationProb > 0)
				{
					BeliefState nextBelief = bs.nextBeliefState(action, observation);
					double error = PEMAErrorBound(vBeliefPoints, nextBelief);
					/* the amount the descendant impacts on total error */
					double weightedError = observationProb * error;
					if (weightedError > maxWeightedError)
					{
						maxWeightedError = weightedError;
						bestBelief = nextBelief;
					}		
				}
			}
			
			
		}
		return bestBelief;	
	}
	
	/***
	 * The error for a belief point, as in PEMA, the last equation in 2.4 
	 * @param vBeliefPoints current set of belief points
	 * @param bprime the belief we are looking at
	 * @return the error
	 */
	private double PEMAErrorBound(Vector<BeliefState> vBeliefPoints, BeliefState bprime)
	{
		double RMAX = m_pPOMDP.getMaxR();
		double RMIN = m_pPOMDP.getMinR();
		double totalError = 0;
		/* find the closest (1-norm) sampled belief to b' */
		
		BeliefState b = m_pPOMDP.getBeliefStateFactory().getNearestL1Belief(vBeliefPoints, bprime);
		AlphaVector a = b.getMaxAlpha();
		
		int state;
		for (state = 0; state < m_pPOMDP.getStateCount(); state++)
		{
			double beliefDifference = bprime.valueAt(state) - b.valueAt(state);
			if (bprime.valueAt(state) >= b.valueAt(state))
			{
				totalError += ((RMAX / (1 - m_pPOMDP.getDiscountFactor())) - a.valueAt(state)) * beliefDifference;				
			}
			else
			{
				totalError += ((RMIN / (1 - m_pPOMDP.getDiscountFactor())) - a.valueAt(state)) * beliefDifference;
			}
			
		}
		
		return totalError;
	}
	
	
}
