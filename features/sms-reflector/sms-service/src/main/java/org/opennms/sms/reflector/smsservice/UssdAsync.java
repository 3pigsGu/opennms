/**
 * 
 */
package org.opennms.sms.reflector.smsservice;

import org.opennms.core.tasks.Async;
import org.opennms.core.tasks.Callback;
import org.smslib.USSDRequest;

public class UssdAsync implements Async<MobileMsgResponse> {
	private final MobileMsgTracker m_tracker;
	private final USSDRequest m_request;
	private final String m_gatewayId;
	private final MobileMsgResponseMatcher m_matcher;
	private long m_timeout;
	private int m_retries;

	public UssdAsync(MobileMsgTracker tracker, String gatewayId, long timeout, int retries, USSDRequest req, MobileMsgResponseMatcher matcher) {
		this.m_tracker = tracker;
		this.m_gatewayId = gatewayId;
		this.m_timeout = timeout;
		this.m_retries = retries;
		this.m_request = req;
		this.m_matcher = matcher;
	}

	public UssdAsync(MobileMsgTracker tracker, String gatewayId, long timeout, int retries, String text, MobileMsgResponseMatcher matcher) {
		this(tracker, gatewayId, timeout, retries, new USSDRequest(text), matcher);
	}

	public void submit(final Callback<MobileMsgResponse> cb) {
		MobileMsgResponseCallback mmrc = new MobileMsgCallbackAdapter(cb);

		try {
			m_tracker.sendUssdRequest(m_gatewayId, m_request, m_timeout, m_retries, mmrc, m_matcher);
		} catch (Exception e) {
			cb.handleException(e);
		}
		
	}
}