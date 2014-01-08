package pomdp.utilities.concurrent;


public class TaskExecutionThread extends Thread{
	protected ThreadPool m_tpPool;
	protected boolean m_bKilled;
	
	public TaskExecutionThread( ThreadPool tp ){
		m_tpPool = tp;
		m_bKilled = false;
	}
	public void run(){
		while( !m_bKilled ){
			Task t = m_tpPool.getNextTask();
			t.execute();
			m_tpPool.taskDone( t );
		}
	}
	public void kill(){
		m_bKilled = true;
	}
}
