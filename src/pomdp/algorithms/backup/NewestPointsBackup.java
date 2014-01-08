package pomdp.algorithms.backup;

import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;

public class NewestPointsBackup extends BackupOrdering {

	/* ordering parameters	*/
	/* iterations of full synchronous backups */
	int numberOfIterations = 1;
	
	
	double epsilon = ExecutionProperties.getEpsilon();
	
	
	public NewestPointsBackup(ValueIteration vi, boolean bReversedBackupOrder)
	{
		super(vi, bReversedBackupOrder);	
	}
	
	public void improveValueFunction(Vector<BeliefState> vOriginalPoints, Vector<BeliefState> vNewBeliefs){
		
		improveValueFunctionSinglePass(vNewBeliefs);	
	}
}
