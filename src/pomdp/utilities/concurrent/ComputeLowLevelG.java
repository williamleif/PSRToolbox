package pomdp.utilities.concurrent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class ComputeLowLevelG extends Task implements Runnable {

	private int m_iAction;
	private int m_iObservation;
	private AlphaVector m_avAlpha;
	private AlphaVector m_avG;
	
	public ComputeLowLevelG( AlphaVector avAlpha, int iAction, int iObservation ){
		m_iAction = iAction;
		m_iObservation = iObservation;
		m_avAlpha = avAlpha.copy();
		m_avG = null;
	}
		
	public ComputeLowLevelG( Element eTask, POMDP pomdp ) throws Exception{
		m_pPOMDP = pomdp;
		setId( Integer.parseInt( eTask.getAttribute( "ID" ) ) );
		m_iAction = Integer.parseInt( eTask.getAttribute( "Action" ) );
		m_iObservation = Integer.parseInt( eTask.getAttribute( "Observation" ) );
		Element eAlphaVector = (Element) eTask.getChildNodes().item( 0 );
		AlphaVector av = AlphaVector.parseDOM( eAlphaVector, m_pPOMDP );
		if( eAlphaVector.getAttribute( "Role" ).equals( "source" ) ){
			m_avAlpha = av;
			m_avG = null;
		}
		else{
			m_avAlpha = null;
			m_avG = av;
		}
	}

	
	public Element getDOM( Document doc ) throws Exception {
		Element eTask = doc.createElement( "ComputeG" ), eAlpha = null;
		eTask.setAttribute( "ID", getId() + "" );
		eTask.setAttribute( "Action", m_iAction +"" );
		eTask.setAttribute( "Observation", m_iObservation +"" );
		if( m_avAlpha != null ){
			eAlpha = m_avAlpha.getDOM( doc );
			eAlpha.setAttribute( "Role", "source" );
		}
		else if( m_avG != null ){
			eAlpha = m_avG.getDOM( doc );
			eAlpha.setAttribute( "Role", "result" );
		}
		eTask.appendChild( eAlpha );
		return eTask;
	}
	
	
	public void run() {
		//Logger.getInstance().log( "FindMaxAlpha", 0, "run", Thread.currentThread().getName() + " Started finding alphas, a = " + m_iAction + ", b = " + m_bsBelief.getSerialNumber() );
		m_avG = m_avAlpha.G( m_iAction, m_iObservation );		
		m_avAlpha = null;
		//Logger.getInstance().log( "FindMaxAlpha", 0, "run", Thread.currentThread().getName() + " Done finding alphas, a = " + m_iAction + ", b = " + m_bsBelief.getSerialNumber() );
	}

	public void execute() {
		//Logger.getInstance().log( "FindMaxAlpha", 0, "run", Thread.currentThread().getName() + " Started finding alphas, a = " + m_iAction + ", b = " + m_bsBelief.getSerialNumber() );
		m_avG = m_avAlpha.G( m_iAction, m_iObservation );
		m_avAlpha = null;
		//Logger.getInstance().log( "FindMaxAlpha", 0, "run", Thread.currentThread().getName() + " Done finding alphas, a = " + m_iAction + ", b = " + m_bsBelief.getSerialNumber() );
	}

	public AlphaVector getG(){
		return m_avG;
	}

	
	public void copyResults( Task tProcessed ){
		m_avG = ((ComputeLowLevelG)tProcessed).getG();		
	}
}
