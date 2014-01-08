package pomdp.utilities.concurrent;

import java.io.Serializable;

import pomdp.environments.POMDP;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class Task implements Serializable {
	
	private int m_iID = -1;
	private boolean m_bDone = false;
	protected POMDP m_pPOMDP = null;
	protected boolean m_bTerminate = false;

	public abstract void execute();

	public void setId( int iID ) {
		m_iID = iID;		
	}

	public int getId() {
		return m_iID;
	}

	public void done(){
		m_bDone = true;
	}
	
	public boolean isDone(){
		return m_bDone;
	}

	public void setPOMDP( POMDP pomdp ){
		m_pPOMDP = pomdp;
		//internalAttach();
	}
	//protected abstract void internalAttach();
	//public abstract void detach();

	public abstract void copyResults( Task tProcessed );
	
	public abstract Element getDOM( Document doc ) throws Exception;
	
	public static Task restoreTask( Document docTask, POMDP pomdp ) throws Exception{
		Element eTask = (Element) docTask.getChildNodes().item( 0 );
		String sName = eTask.getNodeName();
		Task t = null;
		if( sName.equals( "ComputeG" ) ){
			t = new ComputeLowLevelG( eTask, pomdp );
		}
		else if( sName.equals( "FindMaxAlphas" ) ){
			t = new FindMaxAlphas( eTask, pomdp );
		}
		else if( sName.equals( "ComputeDiscountedReward" ) ){
			t = new ComputeDiscountedReward( eTask, pomdp );
		}
		return t;
	}

	public void init() {
		m_bDone = false;		
	}
	public void terminate(){
		m_bTerminate = true;
	}
}
