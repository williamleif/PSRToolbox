package pomdp.algorithms.backup;

import java.util.Iterator;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class PerseusBackup extends BackupOrdering{

	protected Vector<BeliefState> m_vIterationBeliefPoints;
	protected Vector<BeliefState> m_vBeliefPoints;

	/* if we should update all vectors or just the newest ones */
	protected boolean newestOnly;

	public PerseusBackup(ValueIteration vi, boolean vNewestOnly, boolean bReversedBackupOrder)
	{
		super(vi, bReversedBackupOrder);	
		m_vIterationBeliefPoints = null;
		newestOnly = vNewestOnly;
	}



	public void improveValueFunction(Vector<BeliefState> vOriginalPoints, Vector<BeliefState> vNewBeliefs){

		/* initialize the set of belief points */
		m_vBeliefPoints = new Vector<BeliefState>(vNewBeliefs);

		if (!newestOnly)
			m_vBeliefPoints.addAll(vOriginalPoints);

		BeliefState bsCurrent = null;
		AlphaVector avBackup = null;
		double dBackupValue = 0.0;

		initIterationPoints();


		while(!m_vIterationBeliefPoints.isEmpty()){ 

			/* randomly choose a belief point from our current set, Step 2 */
			bsCurrent = chooseNext();
			if(bsCurrent != null){

				//dValue = valueIteration.valueAt(bsCurrent);
				//dValue = bsCurrent.getComputedValue();

				/* compute alpha, Step 2 */
				avBackup = valueIteration.backup(bsCurrent);
				


				/* the value of the belief for the backed up alpha vector */
				dBackupValue = avBackup.dotProduct(bsCurrent);
				m_vIterationBeliefPoints.remove(bsCurrent);
				
				double dDelta = dBackupValue - valueIteration.valueAt(bsCurrent);

				if (dDelta >= 0){
					valueIteration.addPrunePointwiseDominated(avBackup);

					prunePoints(avBackup);
				}
			}
		}
	}



	protected void prunePoints(AlphaVector avNext){
		BeliefState bsCurrent = null;
		Iterator<BeliefState> itPoints = m_vIterationBeliefPoints.iterator();
		double dNewValue = 0.0, dComputedValue = 0.0;
		int cPruned = 0;


		while( itPoints.hasNext() ){
			bsCurrent = (BeliefState)itPoints.next();
			dNewValue = avNext.dotProduct( bsCurrent );
			dComputedValue = bsCurrent.getComputedValue();
			if(dNewValue > dComputedValue){
				itPoints.remove();
				cPruned++;
			}
		}

	}

	protected BeliefState chooseNext() {
		return (BeliefState) valueIteration.choose( m_vIterationBeliefPoints );
	}

	protected boolean iterationComplete() {
		return m_vIterationBeliefPoints.isEmpty();
	}

	protected void initIterationPoints() {
		m_vIterationBeliefPoints = new Vector<BeliefState>( m_vBeliefPoints );
		double dValue = 0.0;

		for (BeliefState bs : m_vBeliefPoints)
		{
			dValue = valueIteration.valueAt(bs);
			bs.setComputedValue( dValue );
		}
	}


}