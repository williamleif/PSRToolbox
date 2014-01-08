package pomdp.utilities;

/**
 * Holds the set of manageable execution properties such as whether operations execution time is counted, whether debug messages are printed.
 * @author shanigu
 *
 */
public class ExecutionProperties {
	private static boolean m_bDebug = false;
	private static boolean m_bReportOperationTime = false;
	private static boolean m_bMultiThread = false;
	private static boolean m_bHighLevelMultiThread = false;
	private static int m_cThreads = 10;
	private static boolean m_bUseRemoteHelpers = false;
	private static boolean m_bEnableDistributedComputing = false;
	
	public static boolean getDebug(){
		return m_bDebug;
	}
	public static boolean getReportOperationTime(){
		return m_bReportOperationTime;
	}
	public static boolean useMultiThread(){
		return m_bMultiThread;
	}
	public static boolean useHighLevelMultiThread(){
		return m_bHighLevelMultiThread;
	}
	public static String getPath(){
		return "Models/";
		//return "D:/POMDP/";
	}
	public static boolean useRemoteHelpers(){
		return m_bUseRemoteHelpers;
	}
	public static boolean enableDistributedComputing() {
		return m_bEnableDistributedComputing;
	}
	public static int getThreadCount(){
		return m_cThreads;
	}
	public static double getEpsilon() {
		return 0.001;
	}
}
