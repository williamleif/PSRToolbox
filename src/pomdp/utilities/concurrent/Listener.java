package pomdp.utilities.concurrent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import pomdp.environments.POMDP;
import pomdp.utilities.Logger;

public class Listener extends Thread {
	
	private int m_iPort = 3000;
	private ServerSocket m_ssExternal;
	private ThreadPool m_tpThreadPool;
	private POMDP m_pPOMDP;
	private boolean m_bKilled;
	
	public Listener( ThreadPool tp, POMDP pomdp ) throws IOException{
		m_tpThreadPool = tp;
		m_ssExternal = new ServerSocket( m_iPort );
		Logger.getInstance().log( "Listender", 0, "Constructor", "Listening on port " + m_iPort );
		m_bKilled = false;
		m_pPOMDP = pomdp;
	}

	public void run(){
		try{
			Socket s = null;
			RemoteTaskExecutionThread t = null;
			while( !m_bKilled ){
				s = m_ssExternal.accept();
				t = new RemoteTaskExecutionThread( m_tpThreadPool, s, m_pPOMDP );
				m_tpThreadPool.addThread( t );
			}
		}
		catch( Exception e ){
			Logger.getInstance().logError( "Listender", "run", "Listener crashed - " + e );			
		}
	}
	
}
