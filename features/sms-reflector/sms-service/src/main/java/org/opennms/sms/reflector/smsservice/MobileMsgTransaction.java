/**
 * 
 */
package org.opennms.sms.reflector.smsservice;

import org.opennms.core.tasks.Async;
import org.opennms.core.tasks.Callback;
import org.opennms.core.tasks.DefaultTaskCoordinator;
import org.opennms.core.tasks.SequenceTask;
import org.opennms.core.tasks.Task;
import org.springframework.core.style.ToStringCreator;

public abstract class MobileMsgTransaction implements Callback<MobileMsgResponse> {
	public static class SmsTransaction extends MobileMsgTransaction {
		private String m_recipient;
		private String m_text;

		public SmsTransaction(String label, String gatewayId, long timeout, int retries, String recipient, String text, MobileMsgResponseMatcher matcher) {
			super(label, gatewayId, timeout, retries, matcher);
			m_recipient = recipient;
			m_text = text;
		}

		@Override
		public Async<MobileMsgResponse> createAsync(MobileMsgTracker tracker) {
			return new SmsAsync(tracker, getGatewayId(), getTimeout(), getRetries(), m_recipient, m_text, getMatcher());
		}

		public String toString() {
			return new ToStringCreator(this)
				.append("label", getLabel())
				.append("recipient", m_recipient)
				.append("text", m_text)
				.append("matcher", getMatcher())
				.toString();
		}
	}

	public static class UssdTransaction extends MobileMsgTransaction {
		private String m_text;

		public UssdTransaction(String label, String gatewayId, long timeout, int retries, String text, MobileMsgResponseMatcher matcher) {
			super(label, gatewayId, timeout, retries, matcher);
			m_text = text;
		}

		@Override
		public Async<MobileMsgResponse> createAsync(MobileMsgTracker tracker) {
			return new UssdAsync(tracker, getGatewayId(), getTimeout(), getRetries(), m_text, getMatcher());
		}

	}


	private String m_label;
	private MobileMsgResponseMatcher m_matcher;
	private Long m_latency;
	private Throwable m_error;
	private long m_timeout;
	private int m_retries;
	private String m_gatewayId;
	
	public MobileMsgTransaction(String label, String gatewayId, long timeout, int retries, MobileMsgResponseMatcher matcher) {
		m_label = label;
		m_gatewayId = gatewayId;
		m_timeout = timeout;
		m_retries = retries;
		m_matcher = matcher;
	}

	public String getLabel() {
		return m_label;
	}

	public String getGatewayId() {
		return m_gatewayId;
	}
	
	public long getTimeout() {
		return m_timeout;
	}
	
	public int getRetries() {
		return m_retries;
	}
	
	public MobileMsgResponseMatcher getMatcher() {
		return m_matcher;
	}

	public Long getLatency() {
		return m_latency;
	}
	
	public Throwable getError() {
		return m_error;
	}

	public Task createTask(MobileMsgTracker tracker, DefaultTaskCoordinator coordinator, SequenceTask sequence) {
		return coordinator.createTask(sequence, createAsync(tracker), this);
	}

	public abstract Async<MobileMsgResponse> createAsync(MobileMsgTracker tracker);

	public void complete(MobileMsgResponse t) {
		if (t != null) {
			m_latency = t.getReceiveTime() - t.getRequest().getSentTime();
		}
	}

	public void handleException(Throwable t) {
		m_error = t;
	}

	public String toString() {
		return new ToStringCreator(this)
			.append("label", m_label)
			.append("matcher", m_matcher)
			.toString();
	}
}