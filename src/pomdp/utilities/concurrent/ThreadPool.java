package pomdp.utilities.concurrent;

import java.util.LinkedList;
import java.util.Vector;

import pomdp.environments.POMDP;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;

public class ThreadPool {
	private Vector<TaskExecutionThread> m_vThreads;
	private LinkedList<Task> m_lTasks;
	private int m_cTasks;
	private POMDP m_pPOMDP;
	
	private Listener m_lListener;
	
	private static ThreadPool g_tpPool = null;

	public ThreadPool( int cThreads, POMDP pomdp ){
		this( cThreads, pomdp, true );
	}
	
	public ThreadPool( int cThreads, POMDP pomdp, boolean bUseRemoteHelpers ){
		int iThread = 0;
		m_pPOMDP = pomdp;
		m_vThreads = new Vector<TaskExecutionThread>();
		m_lTasks = new LinkedList<Task>();
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			m_vThreads.add( new TaskExecutionThread( this ) );
		}
		for( Thread t : m_vThreads ){
			t.start();
		}
		m_cTasks = 0;
		if( bUseRemoteHelpers && ExecutionProperties.useRemoteHelpers() ){
			try{
				m_lListener = new Listener( this, m_pPOMDP );
				m_lListener.start();
			}
			catch( Exception e ){
				Logger.getInstance().logError( "ThreadPool", "Constructor", "Listener could not be started - " + e );
				e.printStackTrace();
			}
		}
	}

	public static ThreadPool getInstance(){
		return g_tpPool;
	}
	public static void createInstance( POMDP pomdp ){
		if( g_tpPool != null ){
			g_tpPool.killAll();
		}
		g_tpPool = new ThreadPool( ExecutionProperties.getThreadCount(), pomdp );
	}
	
	public void killAll() {
		for( TaskExecutionThread t : m_vThreads ){
			t.kill();
		}
		for( TaskExecutionThread t : m_vThreads ){
			addTask( new EmptyTask() );
			addTask( new EmptyTask() );
			addTask( new EmptyTask() );
		}
		for( TaskExecutionThread t : m_vThreads ){
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public int addTask( Task t ){
		t.setId( m_cTasks++ );
		t.init();
		synchronized( g_oTasksLock ){
			m_lTasks.addFirst( t );
		}
		synchronized( m_lTasks ){
			m_lTasks.notify();
		}
		return t.getId();
	}
	
	private Object g_oTasksLock = new Object();
	
	Task getNextTask() {
		Task t = null;
		while( t == null ){
			synchronized( g_oTasksLock ){
				if( m_lTasks.size() > 0 ){
					t = m_lTasks.removeFirst();
				}
			}
			if( t == null ){
				try {
					synchronized( m_lTasks ){
						m_lTasks.wait();
					}
				} 
				catch( InterruptedException e ){
				}
			}
		}
		return t;
	}

	void taskDone( Task t ) {
		synchronized( t ){
			t.done();
			t.notify();
		}
	}
	public void waitForTask( Task t ){
		while( !t.isDone() ){
			try {
				synchronized( t ){
					if( !t.isDone() )
						t.wait();
				}
			} 
			catch (InterruptedException e) {
			}
		}
	}

	public void addThread( RemoteTaskExecutionThread t ){
		m_vThreads.add( t );
		t.start();
	}

	public void clear() {
		killAll();
		m_vThreads.clear();
		m_lTasks.clear();
		m_vThreads = null;
		m_lTasks = null;
		g_tpPool = null;
	}
}
