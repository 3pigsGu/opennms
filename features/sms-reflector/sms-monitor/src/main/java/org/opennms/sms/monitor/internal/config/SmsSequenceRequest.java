package org.opennms.sms.monitor.internal.config;


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.opennms.sms.monitor.MobileSequenceSession;
import org.opennms.sms.reflector.smsservice.MobileMsgResponseHandler;

@XmlRootElement(name="sms-request")
public class SmsSequenceRequest extends MobileSequenceRequest {
	private String m_recipient;

	public SmsSequenceRequest() {
	}

	public SmsSequenceRequest(String label, String text) {
		super(label, text);
	}
	
	public SmsSequenceRequest(String gatewayId, String label, String text) {
		super(gatewayId, label, text);
	}

	@XmlAttribute(name="recipient")
	public String getRecipient() {
		return m_recipient;
	}
	
	public void setRecipient(String recipient) {
		m_recipient = recipient;
	}

    @Override
    public void send(MobileSequenceSession session, MobileMsgResponseHandler responseHandler) {
        session.sendSms(getGatewayIdForRequest(), getRecipient(), getText(), responseHandler);
    }

    public String toString() {
        return new ToStringBuilder(this)
            .append("gatewayId", getGatewayId())
            .append("label", getLabel())
            .append("recipient", getRecipient())
            .append("text", getText())
            .toString();
    }

}
