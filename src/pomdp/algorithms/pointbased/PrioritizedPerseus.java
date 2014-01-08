/*
 * Created on 27/04/2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package pomdp.algorithms.pointbased;

import java.util.Iterator;
import java.util.Vector;

import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;

/**
 * @author admin
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PrioritizedPerseus	extends	PerseusValueIteration {
	
	public PrioritizedPerseus( POMDP pomdp ){
		super( pomdp );
		m_dMinimalProb = 0.0;
	}
		
	protected BeliefState chooseNext(){
		Iterator itPoints = null;
		BeliefState bsCurrent = null, bsMax = null;
		double dError = 0.0, dMaxError = 0.01;
		double dProb = ( 100.0 ) / m_vIterationBeliefPoints.size();
		Vector vTmpBeliefPoints = new Vector( m_vIterationBeliefPoints );
		
		while( ( bsMax == null ) && ( vTmpBeliefPoints.size() > 0 ) ){
			itPoints = vTmpBeliefPoints.iterator();
			while( itPoints.hasNext() ){
				bsCurrent = (BeliefState) itPoints.next();	
				if( m_rndGenerator.nextDouble( 1.0 ) <= dProb ){			
					itPoints.remove();
					dError = computeBellmanError( bsCurrent );
					if( dError > dMaxError ){
						dMaxError = dError;
						bsMax = bsCurrent;
					}
				}
			}
		}
		if( bsMax != null )
			m_vIterationBeliefPoints.remove( bsMax );
		else
			m_vIterationBeliefPoints.clear();
		return bsMax;
	}
	
	public String getName(){
		return "Prioritized Perseus";
	}
	
	protected boolean isDone( double dMaxDelta, double dEpsilon ){
		return dMaxDelta < dEpsilon;
	}
}
