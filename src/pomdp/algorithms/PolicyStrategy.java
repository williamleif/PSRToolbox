/*
 * Created on May 5, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package pomdp.algorithms;

import java.io.Serializable;

import pomdp.utilities.BeliefState;
import pomdp.valuefunction.LinearValueFunctionApproximation;
import pomdp.valuefunction.MDPValueFunction;


/**
 * @author shanigu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class PolicyStrategy/*  implements Serializable*/{
	protected boolean m_bExploring;
	protected boolean m_bStationary;
	
	public PolicyStrategy(){
		m_bExploring = true;
	}
	/**
	 * Returns an action for this belief state following the required policy
	 */
	public abstract int getAction( BeliefState bsCurrent );
	public abstract double getValue( BeliefState bsCurrent );
	public abstract boolean hasConverged();
	public boolean isExploring(){
		return m_bExploring;
	}
	public void setExploration( boolean bActive ){
		m_bExploring = bActive;
	}
	public abstract String getStatus();
	public void setEnvironmentType( boolean bStationary ){
		m_bStationary = bStationary;
	}
	public abstract LinearValueFunctionApproximation getValueFunction();
	public MDPValueFunction getMDPValueFunction() {
		return null;
	}
}
