package org.opennms.web.map.datasources;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Category;
import org.opennms.core.resource.Vault;
import org.opennms.core.resource.db.SimpleDbConnectionFactory;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.web.map.config.MapPropertiesFactory;


public class ServerDataSource implements DataSourceInterface {

	private Map params;
	boolean initialized = false;
	private HashMap severityMapping = new HashMap();
	//private HashMap statusMapping = new HashMap();

	static String LOG4J_CATEGORY = "OpenNMS.Map";
	static Category log;
	
	static final String STATUS_FIELD="ev_status";
	static final String SEVERITY_FIELD="ev_severity";
	static final String TABLE_NAME="v_eventi_snm";
	
	final String CLOSED_STATUS = "CLOSED";
	final String ACK_STATUS = "ACK";
	final String ASSIGNED_STATUS = "ASSIGNED";
	final String OPEN_STATUS = "OPEN";
	
	private static MapPropertiesFactory mpf=null;
	
	static Connection opennmsConn = null;
	static Connection externalConn = null;

	
	public ServerDataSource(){
		ThreadCategory.setPrefix("OpenNMS.Map");
		log = ThreadCategory.getInstance(this.getClass());
		mpf = MapPropertiesFactory.getInstance();
	}
	
	/**
	 * Before invoking get() method, this method must be invoked.
	 */
	public void init(Map params){
		this.params = params;
		log.debug("Init...getting db connection");
	
			try{
				if(opennmsConn==null || opennmsConn.isClosed()){
					opennmsConn = Vault.getDbConnection();
				}
				String url=(String)params.get("url");
				String driver=(String)params.get("driver");
				String user=(String)params.get("user");
				String password=(String)params.get("password");
				//gets external connection 
				if(externalConn==null || externalConn.isClosed()){
					log.debug("getting external db connection with parameters url="+url+", driver="+driver+", user="+user+", password="+password);
					SimpleDbConnectionFactory dbConnFactory = new SimpleDbConnectionFactory();
					dbConnFactory.init(url,driver,user,password);
					externalConn = dbConnFactory.getConnection();
				}				
			}catch(Exception s){
				log.error("Error while getting db Connection from Vault "+s);
				throw new RuntimeException(s);
			}
			
			severityMapping.put("6","Critical");
			severityMapping.put("5","Major");
			severityMapping.put("4","Minor");
			severityMapping.put("3","Warning");
			severityMapping.put("2","Cleared");
			severityMapping.put("1","Normal");
			severityMapping.put("0","Indeterminate");

	}
	
	private boolean isInitialized() throws SQLException {
		
		if (opennmsConn!=null && !opennmsConn.isClosed() && externalConn!=null && !externalConn.isClosed()) return true;
		return false;
	}

	protected void finalize() throws Throwable {
		log.debug("Finalizing...closing db connections");
		super.finalize();
		if(opennmsConn!=null){
			Vault.releaseDbConnection(opennmsConn);
		}
		if(externalConn!=null && !externalConn.isClosed()){
			externalConn.close();
		}
	}
	
	/**
	 * @param params is a HashMap that must contain entries for "url","driver","user","password","Critical","Major","Minor","Warning","Normal","Cleared","Indeterminate"
	 */
	public int getSeverity(Object id){
		ThreadCategory.setPrefix(LOG4J_CATEGORY);
		log = ThreadCategory.getInstance(ServerDataSource.class);
		
		try {
			if (!isInitialized()) init(params);
		} catch (Exception e) {
			log.error("exiting: error found " + e);
			return -1;
		}
		
		int result = -1;
		//get ipaddresses of the node
		HashSet ipAddrs = getIpAddrById(id);
		//If there is no ipaddress for the nodeid
		if(ipAddrs.size()==0){
			log.warn("No ip address found for node with id "+(Integer)id);
			return result;
		}
		// get the severity from external db
		result = getSev(ipAddrs);
		// if no severity is found...
		if(result<0){
			log.error("No severity found for element with id "+(Integer)id);
		}
		return result;
	}

	private HashSet getIpAddrById(Object id){
		//get ipaddresses of the node
		String sqlQueryIFaces= "select distinct ipaddr from ipinterface where ipaddr!='0.0.0.0' and nodeid=?";
		HashSet ipAddrs = new HashSet();
		PreparedStatement ps;
		int nodeId=0;
		
			try {
				nodeId = ((Integer)id).intValue();
				ps = opennmsConn.prepareStatement(sqlQueryIFaces);
				ps.setInt(1, nodeId);
				ResultSet rs = ps.executeQuery();
				while(rs.next()){
					String ipAddr = rs.getString(1);
					ipAddrs.add(ipAddr);
				}	
				rs.close();
				ps.close();
			} catch (SQLException e) {
				log.error("Error while getting ipaddress by id "+e);
			}
		return ipAddrs;
	}

	private int getSev(HashSet ipAddrs){
		int result=-1;
		String getDataQuery="select max("+SEVERITY_FIELD+") from "+TABLE_NAME+" where ip_address in (";
		Iterator it = ipAddrs.iterator();
		while(it.hasNext()){
			String ip = (String) it.next();
			getDataQuery+="'"+ip+"'";
			if(it.hasNext()){
				getDataQuery+=",";
			}
		}
		getDataQuery+=") and "+STATUS_FIELD+"!='"+CLOSED_STATUS+"'";
		log.debug("get severity query is "+getDataQuery);
		String value=null;
		try {
			Statement stmt = externalConn.createStatement();
			ResultSet rs = stmt.executeQuery(getDataQuery);
			// get only first value (if more found)
			
			if(rs.next()){
				value=rs.getString(1);
				log.debug("found severity for ipaddresses "+ipAddrs+" with value "+value);
			}
			rs.close();
			stmt.close();
		} catch (SQLException e1) {
			log.error("Exception while getting severity "+e1);
			return -1;
		}
		
		String sevLabel = (String)severityMapping.get(value);
		log.debug("Getting severity mapping for key="+value+": sevLabel="+sevLabel);
		
		try {
			result = mpf.getSeverity(sevLabel);
			log.debug("Got severity:"+result );
		} catch (Exception e) {
			log.error("No severity found for severity label "+sevLabel+ " "+e);
			result=-1;
		}
		return result;
	}

	
	public int getStatus(Object id){
		ThreadCategory.setPrefix(LOG4J_CATEGORY);
		log = ThreadCategory.getInstance(ServerDataSource.class);
		
		try {
			if (!isInitialized()) init(params);
		} catch (Exception e) {
			log.error("exiting: error found " + e);
			return -1;
		}
		
		int result = -1;
		//get ipaddresses of the node
		HashSet ipAddrs = getIpAddrById(id);
		//If there is no ipaddress for the nodeid
		if(ipAddrs.size()==0){
			log.warn("No ip address found for node with id "+(Integer)id);
			return result;
		}
		// get the severity from external db
		result = getSt(ipAddrs);
		// if no severity is found...
		if(result<0){
			log.error("No severity found for element with id "+(Integer)id);
		}
		return result;

	}
	
	private int getSt(HashSet ipAddrs){
		int result=-1;
		String getDataQuery="select "+STATUS_FIELD+" from "+TABLE_NAME+" where ip_address in (";
		Iterator it = ipAddrs.iterator();
		while(it.hasNext()){
			String ip = (String) it.next();
			getDataQuery+="'"+ip+"'";
			if(it.hasNext()){
				getDataQuery+=",";
			}
		}
		getDataQuery+=") and "+STATUS_FIELD+"!='"+CLOSED_STATUS+"'";
		String innerQuery = "select max("+SEVERITY_FIELD+") from "+TABLE_NAME+" where ip_address in (";
		
		Iterator it2 = ipAddrs.iterator();
		while(it2.hasNext()){
			String ip = (String) it2.next();
			innerQuery+="'"+ip+"'";
			if(it2.hasNext()){
				innerQuery+=",";
			}
		}
		innerQuery+=") and "+STATUS_FIELD+"!='"+CLOSED_STATUS+"'";
		getDataQuery+=" and "+SEVERITY_FIELD+"=("+innerQuery+")" ;
		
		log.debug("get status query is "+getDataQuery);
		String value=null;
		try {
			Statement stmt = externalConn.createStatement();
			ResultSet rs = stmt.executeQuery(getDataQuery);
			// get only first value (if more found)
			
			if(rs.next()){
				value=rs.getString(1);
				log.debug("found status for ipaddresses "+ipAddrs+" with value "+value);
			}
			rs.close();
			stmt.close();
		} catch (SQLException e1) {
			log.error("Exception while getting status "+e1);
			return -1;
		}
		
		try {
			result = mpf.getStatus(value);
			log.debug("Got status:"+result );
		} catch (Exception e) {
			log.error("No status found for status label "+value+ " "+e);
			result=-1;
		}
		return result;
	}
	
	public double getAvailability(Object id) {
		// not implemented
		return -1;
	}


}
