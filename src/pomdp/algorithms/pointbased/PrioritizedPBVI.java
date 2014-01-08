package pomdp.algorithms.pointbased;

import java.util.Iterator;
import java.util.Vector;

import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.Logger;

public class PrioritizedPBVI extends PointBasedValueIteration {
	
	public PrioritizedPBVI( POMDP pomdp ){
		super( pomdp );
	}
	
	protected double improveValueFunction( Vector vBeliefPoints ){
		Iterator itPoints = null;
		BeliefState bsCurrent = null, bsMax = null, bsLast = null;
		double dError = 0.0, dMaxError = 0.0;
		boolean bDone = false;
		AlphaVector avBackup = null;
		int iIteration = 0, cIterations = vBeliefPoints.size();
		Vector vTmpBeliefPoints = null;
		double dProb = 500.0 / vBeliefPoints.size();
		double dOldValue = 0.0, dNewValue = 0.0;
		
		if( cIterations > 25 )
			cIterations = 25;
		
		for( iIteration = 0 ; ( iIteration < cIterations ) && ! bDone ; iIteration++ ){	
			vTmpBeliefPoints = new Vector( vBeliefPoints );
			bsMax = null;
			dMaxError = 0.02;
			
			while( ( vTmpBeliefPoints.size() > 0 ) && ( bsMax == null ) ){
				itPoints = vTmpBeliefPoints.iterator();
				while( itPoints.hasNext() ){
					bsCurrent = (BeliefState) itPoints.next();
					if( m_rndGenerator.nextDouble() < dProb ){
						dError = computeBellmanError( bsCurrent );
						if( dError > dMaxError ){
							dMaxError = dError;
							bsMax = bsCurrent;
						}
						itPoints.remove();
					}
				}
				
			}
			
			if( bsMax != null ){
				avBackup = backup( bsMax );
				bsLast = bsMax;
				dOldValue = m_vValueFunction.valueAt( bsMax );
				m_vValueFunction.addPrunePointwiseDominated( avBackup );
				dNewValue = m_vValueFunction.valueAt( bsMax );
			}
			else{
				bDone = true;
			}
		}
		Logger.getInstance().logln( "Last delta over " + bsLast + 
				" from " + round( dOldValue, 3 ) + 
				" to " + round( dNewValue, 3 ) );
		
		return dNewValue - dOldValue;
	}

	protected void addNoPrune( AlphaVector avNew, Vector vValueFunction ){
		Iterator it = vValueFunction.iterator();
		AlphaVector avExisting = null;
		BeliefState bsWitness = null;
		
		while( it.hasNext() ){
			avExisting = (AlphaVector)it.next();
			bsWitness = avExisting.getWitness();
			if( bsWitness == avNew.getWitness() ){
				it.remove();
			}
		}
		vValueFunction.add( avNew );
	}
	
	public String getName(){
		return "PPBVI";
	}
}
