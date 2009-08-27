package org.opennms.sms.gateways.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.opennms.sms.reflector.smsservice.GatewayGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smslib.AGateway;
import org.smslib.AGateway.Protocols;
import org.smslib.modem.SerialModemGateway;
import org.springframework.beans.factory.InitializingBean;

public class GatewayGroupLoader implements InitializingBean {
    
    private static Logger log = LoggerFactory.getLogger(GatewayGroupLoader.class); 

    private URL m_configURL;
    private GatewayGroup[] m_gatewayGroups;
    private GatewayGroupRegistrar m_gatewayGroupRegistrar;
	
    public GatewayGroupLoader(GatewayGroupRegistrar gatewayGroupRegistrar, URL configURL) {
        m_gatewayGroupRegistrar = gatewayGroupRegistrar;
    	m_configURL = configURL;
        
        
    }
    
    public GatewayGroup[] getGatewayGroups() {
        return m_gatewayGroups;
    }
	
    public void load() {

        Properties modemProperties = new Properties();
        InputStream in = null;

        try{
            in = m_configURL.openStream();
            modemProperties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        String modems = System.getProperty("org.opennms.sms.gateways.modems");
        
        if (modems == null || "".equals(modems.trim())) {
            modems = modemProperties.getProperty("modems");
        }

        String[] tokens = modems.split("\\s");

        final AGateway[] gateways = new AGateway[tokens.length];

        if (tokens.length == 0) {
            m_gatewayGroups = new GatewayGroup[0];
        } else {

            for(int i = 0; i < tokens.length; i++){
                String modemId = tokens[i];
                String port = modemProperties.getProperty(modemId + ".port");
                if (port == null) {
                    throw new IllegalArgumentException("No port defined for modem with id " + modemId + " in " + m_configURL);
                }
                int baudRate = Integer.parseInt(modemProperties.getProperty(modemId + ".baudrate", "9600"));
                String manufacturer = modemProperties.getProperty(modemId + ".manufacturer");
                String model = modemProperties.getProperty(modemId + ".model");
                String pin = modemProperties.getProperty(modemId+".pin", "0000");

                infof("Create SerialModemGateway(%s, %s, %d, %s, %s)", modemId, port, baudRate, manufacturer, model);

                SerialModemGateway gateway = new SerialModemGateway(modemId, port, baudRate, manufacturer, model);
                gateway.setProtocol(Protocols.PDU);
                gateway.setInbound(true);
                gateway.setOutbound(true);
                gateway.setSimPin(pin);

                gateways[i] = gateway;
            }


            GatewayGroup gatewayGroup = new GatewayGroup() {

                public AGateway[] getGateways() {
                    return gateways;
                }

            };

            m_gatewayGroups  = new GatewayGroup[] { gatewayGroup };

        }

    }
	
	private void infof(String fmt, Object... args) {
	    if (log.isInfoEnabled()) {
	        log.info(String.format(fmt, args));
	    }
	}

	public void afterPropertiesSet() throws Exception {
		load();
		for(GatewayGroup group : getGatewayGroups()){
			m_gatewayGroupRegistrar.registerGatewayGroup(group);
		}
	}
}
