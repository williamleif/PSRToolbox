package pomdp.algorithms.backup;

import java.util.Collections;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.Logger;
import pomdp.valuefunction.LinearValueFunctionApproximation;


public abstract class BackupOrdering {

	POMDP POMDP;
	ValueIteration valueIteration;
	LinearValueFunctionApproximation VFprevious;
	protected boolean m_bReversedBackupOrder;
	public BackupOrdering(ValueIteration vi, boolean bReversedBackupOrder)
	{
		valueIteration = vi;		
		POMDP = vi.getPOMDP();
		m_bReversedBackupOrder = bReversedBackupOrder;
	}
	
	/***
	 * Improve the value function by using the inputted set of Belief Points
	 * @param The previous set of beliefs
	 * @param The belief points that were added in the most recent iteration of expansion (if any)
	 * @return
	 */
	public abstract void improveValueFunction(Vector<BeliefState> vBeliefPoints, Vector<BeliefState> vNewBeliefPoints);
	
	public void improveValueFunction(LinearValueFunctionApproximation oldVF, Vector<BeliefState> vBeliefPoints, Vector<BeliefState> vNewBeliefPoints, int numIterations)
	{
		VFprevious = oldVF;
		for (int i = 0; i < numIterations; i++)
		{
			improveValueFunction(vBeliefPoints, vNewBeliefPoints);
			VFprevious = valueIteration.getValueFunction();
		}
	}
	
	
	
	
	public void improveValueFunctionSinglePass(Vector<BeliefState> vBeliefPoints){
		
		//LinearValueFunctionApproximation vNextValueFunction = new LinearValueFunctionApproximation(valueIteration.getEpsilon(), true );
		AlphaVector avBackup = null;
		/* iterate through all points */
		int iBelief = 0;
		BeliefState bsCurrent = null;
		if(m_bReversedBackupOrder)
			Collections.reverse(vBeliefPoints);
		for ( iBelief = 0 ; iBelief < vBeliefPoints.size() ; iBelief++ )
		{
			bsCurrent = vBeliefPoints.elementAt( iBelief );
			avBackup = valueIteration.backup(bsCurrent);
			
			/* the value of the belief for the backed up alpha vector */
			double dBackupValue = avBackup.dotProduct(bsCurrent);

			double dDelta = dBackupValue - valueIteration.valueAt(bsCurrent);

			
			if (dDelta >= 0)
				valueIteration.addPrunePointwiseDominated(avBackup);
			//if( iBelief % 10 == 0 ){
				//System.gc();
			//	Logger.getInstance().logFull("BackupOrdering", 0, "improveValueFunctionSinglePass", "Done " + (vBeliefPoints.size() - iBelief) + " points" );
			//}
		}
	}
	
	public void buildNewValueFunctionSinglePass(Vector<BeliefState> vBeliefPoints){

		LinearValueFunctionApproximation vNextValueFunction = new LinearValueFunctionApproximation(valueIteration.getEpsilon(), true );
		AlphaVector avBackup = null;
		/* iterate through all points */
		for (BeliefState bsCurrent : vBeliefPoints)
		{
			avBackup = valueIteration.backup(bsCurrent, VFprevious);
			
			
			/* the value of the belief for the backed up alpha vector */
			double dBackupValue = avBackup.dotProduct(bsCurrent);

			double dDelta = dBackupValue - valueIteration.valueAt(bsCurrent);

			if (dDelta > 0)
				vNextValueFunction.addPrunePointwiseDominated(avBackup);
			else
				vNextValueFunction.addPrunePointwiseDominated(valueIteration.getMaxAlpha(bsCurrent));
			
		}
		valueIteration.getValueFunction().copy(vNextValueFunction);
	}


	public String toString()
	{
		return this.getClass().getSimpleName();
	}

	public String isReversed() {
		if(m_bReversedBackupOrder)
			return "ReversedBackup";
		else
			return "ForwardBackup";
	}

}
