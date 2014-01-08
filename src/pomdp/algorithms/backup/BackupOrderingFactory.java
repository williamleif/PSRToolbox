package pomdp.algorithms.backup;

import pomdp.algorithms.ValueIteration;

public class BackupOrderingFactory {

	public static BackupOrdering getBackupOrderingAlgorithm(String sName, ValueIteration vi, boolean bReversedBackupOrder){


		if(sName.equals("FullBackup"))
			return new FullBackup(vi, bReversedBackupOrder);
		if(sName.equals("NewestPointsBackup"))
			return new NewestPointsBackup(vi, bReversedBackupOrder);
		if(sName.equals("NewestPerseusBackup"))
			return new PerseusBackup(vi, true, bReversedBackupOrder);
		if(sName.equals("FullPerseusBackup") || sName.equals("PerseusBackup"))
			return new PerseusBackup(vi, false, bReversedBackupOrder);
		
		return null;
	}
}
