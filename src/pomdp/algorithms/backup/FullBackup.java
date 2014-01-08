package pomdp.algorithms.backup;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;

public class FullBackup extends BackupOrdering {


	public FullBackup(ValueIteration vi, boolean bReversedBackupOrder)
	{
		super(vi, bReversedBackupOrder);	
	}
	
	public void improveValueFunction(Vector<BeliefState> vOriginalPoints, Vector<BeliefState> vNewBeliefs){
		
		/* we will operate on all belief points */
		Vector<BeliefState> vBeliefPoints = new Vector<BeliefState>(vOriginalPoints);
		vBeliefPoints.addAll(vNewBeliefs);
		
		improveValueFunctionSinglePass(vBeliefPoints);
		//buildNewValueFunctionSinglePass(vBeliefPoints);	

	}
}
