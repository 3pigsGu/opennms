package org.opennms.netmgt.dao;

import java.util.Date;

import org.opennms.netmgt.dao.db.AbstractTransactionalTemporaryDatabaseSpringContextTests;
import org.opennms.netmgt.dao.hibernate.LocationMonitorDaoHibernate;
import org.opennms.netmgt.model.NetworkBuilder;
import org.opennms.netmgt.model.OnmsDistPoller;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsMonitoredService;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsOutage;
import org.opennms.netmgt.model.OnmsServiceType;

public class AbstractTransactionalDaoTestCase extends AbstractTransactionalTemporaryDatabaseSpringContextTests {

    private DistPollerDao m_distPollerDao;
    private NodeDao m_nodeDao;
    private IpInterfaceDao m_ipInterfaceDao;
    private SnmpInterfaceDao m_snmpInterfaceDao;
    private MonitoredServiceDao m_monitoredServiceDao;
    private ServiceTypeDao m_serviceTypeDao;
    private AssetRecordDao m_assetRecordDao;
    private CategoryDao m_categoryDao;
    private OutageDao m_outageDao;
    private EventDao m_eventDao;
    private AlarmDao m_alarmDao;
    private NotificationDao m_notificationDao;
    private UserNotificationDao m_userNotificationDao;
    private AvailabilityReportLocatorDao m_availabilityReportLocatorDao;
    private LocationMonitorDaoHibernate m_locationMonitorDao;
    
    private OnmsNode m_node1;
    private boolean m_populate = true;
    
    public AbstractTransactionalDaoTestCase() {
        System.setProperty("opennms.home", "../opennms-daemon/src/main/filtered");
    }
    
    @Override
    protected String[] getConfigLocations() {
        return new String[] {
                "classpath:/META-INF/opennms/applicationContext-dao.xml"
        };
    }

    @Override
    protected void onSetUpInTransactionIfEnabled() {
    	populateDatabase();
    }
    
    private void populateDatabase() {
    	if (!m_populate) {
    	    return;
    	}
    	
        //OnmsDistPoller distPoller = dao.load("localhost");

        getServiceTypeDao().save(new OnmsServiceType("ICMP"));
        getServiceTypeDao().flush();
        getServiceTypeDao().save(new OnmsServiceType("SNMP"));
        getServiceTypeDao().flush();
        getServiceTypeDao().save(new OnmsServiceType("HTTP"));
        getServiceTypeDao().flush();
        
        OnmsDistPoller distPoller = new OnmsDistPoller("localhost", "127.0.0.1");
        getDistPollerDao().save(distPoller);
        getDistPollerDao().flush();
        
        NetworkBuilder builder = new NetworkBuilder(distPoller);
        m_node1 = builder.addNode("node1").setForeignSource("imported:").setForeignId("1").getNode();
        assertNotNull("newly built node 1 should not be null", m_node1);
        builder.addInterface("192.168.1.1").setIsManaged("M").setIsSnmpPrimary("P").setIpStatus(1).addSnmpInterface("192.168.1.1", 1).setIfSpeed(10000000);
        getNodeDao().save(builder.getCurrentNode());
        getNodeDao().flush();
        builder.addService(getServiceType("ICMP"));
        builder.addService(getServiceType("SNMP"));
        builder.addInterface("192.168.1.2").setIsManaged("M").setIsSnmpPrimary("S").setIpStatus(1).addSnmpInterface("192.168.1.2", 2).setIfSpeed(10000000);
        builder.addService(getServiceType("ICMP"));
        builder.addService(getServiceType("HTTP"));
        builder.addInterface("192.168.1.3").setIsManaged("M").setIsSnmpPrimary("N").setIpStatus(1).addSnmpInterface("192.168.1.3", 3).setIfSpeed(10000000);
        builder.addService(getServiceType("ICMP"));
        getNodeDao().save(builder.getCurrentNode());
        getNodeDao().flush();
        
        builder.addNode("node2").setForeignSource("imported:").setForeignId("2");
        builder.addInterface("192.168.2.1").setIsManaged("M").setIsSnmpPrimary("P").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        builder.addService(getServiceType("SNMP"));
        builder.addInterface("192.168.2.2").setIsManaged("M").setIsSnmpPrimary("S").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        builder.addService(getServiceType("HTTP"));
        builder.addInterface("192.168.2.3").setIsManaged("M").setIsSnmpPrimary("N").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        getNodeDao().save(builder.getCurrentNode());
        getNodeDao().flush();
        
        builder.addNode("node3").setForeignSource("imported:").setForeignId("3");
        builder.addInterface("192.168.3.1").setIsManaged("M").setIsSnmpPrimary("P").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        builder.addService(getServiceType("SNMP"));
        builder.addInterface("192.168.3.2").setIsManaged("M").setIsSnmpPrimary("S").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        builder.addService(getServiceType("HTTP"));
        builder.addInterface("192.168.3.3").setIsManaged("M").setIsSnmpPrimary("N").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        getNodeDao().save(builder.getCurrentNode());
        getNodeDao().flush();
        
        builder.addNode("node4").setForeignSource("imported:").setForeignId("4");
        builder.addInterface("192.168.4.1").setIsManaged("M").setIsSnmpPrimary("P").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        builder.addService(getServiceType("SNMP"));
        builder.addInterface("192.168.4.2").setIsManaged("M").setIsSnmpPrimary("S").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        builder.addService(getServiceType("HTTP"));
        builder.addInterface("192.168.4.3").setIsManaged("M").setIsSnmpPrimary("N").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        getNodeDao().save(builder.getCurrentNode());
        getNodeDao().flush();

        //This node purposely doesn't have a foreignId style assetNumber
        builder.addNode("alternate-node1").getAssetRecord().setAssetNumber("5");
        builder.addInterface("10.1.1.1").setIsManaged("M").setIsSnmpPrimary("P").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        builder.addService(getServiceType("SNMP"));
        builder.addInterface("10.1.1.2").setIsManaged("M").setIsSnmpPrimary("S").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        builder.addService(getServiceType("HTTP"));
        builder.addInterface("10.1.1.3").setIsManaged("M").setIsSnmpPrimary("N").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        getNodeDao().save(builder.getCurrentNode());
        getNodeDao().flush();
        
        //This node purposely doesn't have a assetNumber and is used by a test to check the category
        builder.addNode("alternate-node2").getAssetRecord().setDisplayCategory("category1");
        builder.addInterface("10.1.2.1").setIsManaged("M").setIsSnmpPrimary("P").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        builder.addService(getServiceType("SNMP"));
        builder.addInterface("10.1.2.2").setIsManaged("M").setIsSnmpPrimary("S").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        builder.addService(getServiceType("HTTP"));
        builder.addInterface("10.1.2.3").setIsManaged("M").setIsSnmpPrimary("N").setIpStatus(1);
        builder.addService(getServiceType("ICMP"));
        getNodeDao().save(builder.getCurrentNode());
        getNodeDao().flush();
        
        OnmsEvent event = new OnmsEvent();
        event.setDistPoller(distPoller);
        event.setEventUei("uei.opennms.org/test");
        event.setEventTime(new Date());
        event.setEventSource("test");
        event.setEventCreateTime(new Date());
        event.setEventSeverity(1);
        event.setEventLog("Y");
        event.setEventDisplay("Y");
        getEventDao().save(event);
        getEventDao().flush();
       
        OnmsMonitoredService svc = getMonitoredServiceDao().get(1, "192.168.1.1", "SNMP");
        OnmsOutage resolved = new OnmsOutage(new Date(), new Date(), event, event, svc, null, null);
        getOutageDao().save(resolved);
        getOutageDao().flush();
        
        OnmsOutage unresolved = new OnmsOutage(new Date(), event, svc);
        getOutageDao().save(unresolved);
        getOutageDao().flush();
    }
    
    private OnmsServiceType getServiceType(String svcName) {
        OnmsServiceType svcType = getServiceTypeDao().findByName(svcName);
        return svcType;
    }


    public AlarmDao getAlarmDao() {
        return m_alarmDao;
    }


    public void setAlarmDao(AlarmDao alarmDao) {
        m_alarmDao = alarmDao;
    }


    public AssetRecordDao getAssetRecordDao() {
        return m_assetRecordDao;
    }


    public void setAssetRecordDao(AssetRecordDao assetRecordDao) {
        m_assetRecordDao = assetRecordDao;
    }


    public AvailabilityReportLocatorDao getAvailabilityReportLocatorDao() {
        return m_availabilityReportLocatorDao;
    }


    public void setAvailabilityReportLocatorDao(
            AvailabilityReportLocatorDao availabilityReportLocatorDao) {
        m_availabilityReportLocatorDao = availabilityReportLocatorDao;
    }


    public CategoryDao getCategoryDao() {
        return m_categoryDao;
    }


    public void setCategoryDao(CategoryDao categoryDao) {
        m_categoryDao = categoryDao;
    }


    public DistPollerDao getDistPollerDao() {
        return m_distPollerDao;
    }


    public void setDistPollerDao(DistPollerDao distPollerDao) {
        m_distPollerDao = distPollerDao;
    }


    public EventDao getEventDao() {
        return m_eventDao;
    }


    public void setEventDao(EventDao eventDao) {
        m_eventDao = eventDao;
    }


    public IpInterfaceDao getIpInterfaceDao() {
        return m_ipInterfaceDao;
    }


    public void setIpInterfaceDao(IpInterfaceDao ipInterfaceDao) {
        m_ipInterfaceDao = ipInterfaceDao;
    }


    public MonitoredServiceDao getMonitoredServiceDao() {
        return m_monitoredServiceDao;
    }


    public void setMonitoredServiceDao(MonitoredServiceDao monitoredServiceDao) {
        m_monitoredServiceDao = monitoredServiceDao;
    }


    public NodeDao getNodeDao() {
        return m_nodeDao;
    }


    public void setNodeDao(NodeDao nodeDao) {
        m_nodeDao = nodeDao;
    }


    public NotificationDao getNotificationDao() {
        return m_notificationDao;
    }


    public void setNotificationDao(NotificationDao notificationDao) {
        m_notificationDao = notificationDao;
    }


    public OutageDao getOutageDao() {
        return m_outageDao;
    }


    public void setOutageDao(OutageDao outageDao) {
        m_outageDao = outageDao;
    }


    public ServiceTypeDao getServiceTypeDao() {
        return m_serviceTypeDao;
    }


    public void setServiceTypeDao(ServiceTypeDao serviceTypeDao) {
        m_serviceTypeDao = serviceTypeDao;
    }


    public SnmpInterfaceDao getSnmpInterfaceDao() {
        return m_snmpInterfaceDao;
    }


    public void setSnmpInterfaceDao(SnmpInterfaceDao snmpInterfaceDao) {
        m_snmpInterfaceDao = snmpInterfaceDao;
    }


    public UserNotificationDao getUserNotificationDao() {
        return m_userNotificationDao;
    }


    public void setUserNotificationDao(UserNotificationDao userNotificationDao) {
        m_userNotificationDao = userNotificationDao;
    }
    
    public OnmsNode getNode1() {
        return m_node1;
    }

    public LocationMonitorDaoHibernate getLocationMonitorDao() {
        return m_locationMonitorDao;
    }

    public void setLocationMonitorDao(LocationMonitorDaoHibernate locationMonitorDao) {
        m_locationMonitorDao = locationMonitorDao;
    }

    public boolean isPopulate() {
        return m_populate;
    }

    public void setPopulate(boolean populate) {
        m_populate = populate;
    }
}
