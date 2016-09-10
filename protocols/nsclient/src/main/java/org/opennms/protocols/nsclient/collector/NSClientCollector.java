/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2007-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.protocols.nsclient.collector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.collection.api.CollectionAgent;
import org.opennms.netmgt.collection.api.CollectionSet;
import org.opennms.netmgt.collection.api.ServiceCollector;
import org.opennms.netmgt.collection.api.ServiceParameters;
import org.opennms.netmgt.collection.support.NumericAttributeUtils;
import org.opennms.netmgt.collection.support.builder.CollectionSetBuilder;
import org.opennms.netmgt.collection.support.builder.CollectionStatus;
import org.opennms.netmgt.collection.support.builder.NodeLevelResource;
import org.opennms.netmgt.config.datacollction.nsclient.Attrib;
import org.opennms.netmgt.config.datacollction.nsclient.NsclientCollection;
import org.opennms.netmgt.config.datacollction.nsclient.Wpm;
import org.opennms.netmgt.events.api.EventProxy;
import org.opennms.netmgt.rrd.RrdRepository;
import org.opennms.protocols.nsclient.NSClientAgentConfig;
import org.opennms.protocols.nsclient.NsclientCheckParams;
import org.opennms.protocols.nsclient.NsclientException;
import org.opennms.protocols.nsclient.NsclientManager;
import org.opennms.protocols.nsclient.NsclientPacket;
import org.opennms.protocols.nsclient.config.NSClientDataCollectionConfigFactory;
import org.opennms.protocols.nsclient.config.NSClientPeerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>NSClientCollector class.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public class NSClientCollector implements ServiceCollector {
	
	private static final Logger LOG = LoggerFactory.getLogger(NSClientCollector.class);

    // Don't make this static because each service will have its own
    // copy and the key won't require the service name as part of the key.
    private final Map<Integer, NSClientAgentState> m_scheduledNodes = new HashMap<Integer, NSClientAgentState>();

    /** {@inheritDoc} */
    @Override
    public CollectionSet collect(CollectionAgent agent, EventProxy eproxy, Map<String, Object> parameters) {
        CollectionSetBuilder builder = new CollectionSetBuilder(agent);
        builder.withStatus(CollectionStatus.SUCCEEDED);
        final ServiceParameters serviceParams = new ServiceParameters(parameters);
        String collectionName = serviceParams.getCollectionName();

        // Find attributes to collect - check groups in configuration. For each,
        // check scheduled nodes to see if that group should be collected
        NsclientCollection collection = NSClientDataCollectionConfigFactory.getInstance().getNSClientCollection(collectionName);
        NSClientAgentState agentState = m_scheduledNodes.get(agent.getNodeId());

        // All node resources for NSClient; nothing of interface or "indexed resource" type
        NodeLevelResource nodeResource = new NodeLevelResource(agent.getNodeId());
        for (Wpm wpm : collection.getWpms().getWpm()) {
            // A wpm consists of a list of attributes, identified by name
            if (agentState.shouldCheckAvailability(wpm.getName(), wpm.getRecheckInterval())) {
                LOG.debug("Checking availability of group {}", wpm.getName());
                NsclientManager manager = null;
                try {
                    manager = agentState.getManager();
                    manager.init();
                    NsclientCheckParams params = new NsclientCheckParams(wpm.getKeyvalue());
                    NsclientPacket result = manager.processCheckCommand(NsclientManager.CHECK_COUNTER, params);
                    manager.close();
                    boolean isAvailable = (result.getResultCode() == NsclientPacket.RES_STATE_OK);
                    agentState.setGroupIsAvailable(wpm.getName(), isAvailable);
                    LOG.debug("Group {} is {}available ", wpm.getName(), (isAvailable?"":"not"));
                } catch (NsclientException e) {
                    LOG.error("Error checking group ({}) availability", wpm.getName(), e);
                    agentState.setGroupIsAvailable(wpm.getName(), false);
                } finally {
                    if (manager != null) {
                        manager.close();
                    }
                }
            }

            if (agentState.groupIsAvailable(wpm.getName())) {
                // Collect the data
                try {
                    NsclientManager manager = agentState.getManager();
                    manager.init(); // Open the connection, then do each
                                    // attribute

                    for (Attrib attrib : wpm.getAttrib()) {
                        NsclientPacket result = null;

                        try {
                            NsclientCheckParams params = new NsclientCheckParams(attrib.getName());
                            result = manager.processCheckCommand(NsclientManager.CHECK_COUNTER, params);
                        } catch (NsclientException e) {
                            LOG.info("unable to collect params for attribute '{}'", attrib.getName(), e);
                        }

                        if (result != null) {
                            if (result.getResultCode() != NsclientPacket.RES_STATE_OK) {
                                LOG.info("not writing parameters for attribute '{}', state is not 'OK'", attrib.getName());
                            } else {
                                // Only numeric data comes back from NSClient in data collection
                                builder.withNumericAttribute(nodeResource, wpm.getName(), attrib.getAlias(), NumericAttributeUtils.parseNumericValue(result.getResponse()), attrib.getType());
                            }
                        }
                    }
                    builder.withStatus(CollectionStatus.SUCCEEDED);
                    manager.close(); // Only close once all the attribs have
                                        // been done (optimizing as much as
                                        // possible with NSClient)
                } catch (NsclientException e) {
                    LOG.error("Error collecting data", e);
                }
            }
        }
        return builder.build();
    }

    /** {@inheritDoc} */
    @Override
    public void initialize(Map<String, String> parameters) {
        LOG.debug("initialize: Initializing NSClientCollector.");
        m_scheduledNodes.clear();
        initNSClientPeerFactory();
        initNSClientCollectionConfig();
        initializeRrdRepository();
    }

    private static void initNSClientPeerFactory() {
        LOG.debug("initialize: Initializing NSClientPeerFactory");
        try {
            NSClientPeerFactory.init();
        } catch (IOException e) {
            LOG.error("initialize: Error reading configuration", e);
            throw new UndeclaredThrowableException(e);
        }
    }

    private static void initNSClientCollectionConfig() {
        LOG.debug("initialize: Initializing collector: {}", NSClientCollector.class);
        try {
            NSClientDataCollectionConfigFactory.init();
        } catch (FileNotFoundException e) {
            LOG.error("initialize: Error locating configuration.", e);
            throw new UndeclaredThrowableException(e);
        } catch (IOException e) {
            LOG.error("initialize: Error reading configuration", e);
            throw new UndeclaredThrowableException(e);
        }
    }

    private static void initializeRrdRepository() {
        LOG.debug("initializeRrdRepository: Initializing RRD repo from NSClientCollector...");
        initializeRrdDirs();
    }

    private static void initializeRrdDirs() {
        /*
         * If the RRD file repository directory does NOT already exist, create
         * it.
         */
        File f = new File(NSClientDataCollectionConfigFactory.getInstance().getRrdPath());
        if (!f.isDirectory()) {
            if (!f.mkdirs()) {
                throw new RuntimeException("Unable to create RRD file " + "repository.  Path doesn't already exist and could not make directory: " + NSClientDataCollectionConfigFactory.getInstance().getRrdPath());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void initialize(CollectionAgent agent, Map<String, Object> parameters) {
        LOG.debug("initialize: Initializing NSClient collection for agent: {}", agent);
        Integer scheduledNodeKey = agent.getNodeId();
        NSClientAgentState nodeState = m_scheduledNodes.get(scheduledNodeKey);

        if (nodeState != null) {
            LOG.info("initialize: Not scheduling interface for NSClient collection: {}", nodeState.getAddress());
            final StringBuffer sb = new StringBuffer();
            sb.append("initialize service: ");

            sb.append(" for address: ");
            sb.append(nodeState.getAddress());
            sb.append(" already scheduled for collection on node: ");
            sb.append(agent);
            LOG.debug(sb.toString());
            throw new IllegalStateException(sb.toString());
        } else {
            nodeState = new NSClientAgentState(agent.getAddress(), parameters);
            LOG.info("initialize: Scheduling interface for collection: {}", nodeState.getAddress());
            m_scheduledNodes.put(scheduledNodeKey, nodeState);
        }
    }

    /**
     * <p>release</p>
     */
    @Override
    public void release() {
        m_scheduledNodes.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void release(final CollectionAgent agent) {
        final Integer scheduledNodeKey = agent.getNodeId();
        NSClientAgentState nodeState = m_scheduledNodes.get(scheduledNodeKey);
        if (nodeState != null) {
            m_scheduledNodes.remove(scheduledNodeKey);
        }
    }

    private static class NSClientAgentState {
        private final NsclientManager m_manager;
        private final NSClientAgentConfig m_agentConfig; // Do we need to keep this?
        private final String m_address;
        private final Map<String, NSClientGroupState> m_groupStates = new HashMap<String, NSClientGroupState>();

        public NSClientAgentState(InetAddress address, Map<String, Object> parameters) {
            m_address = InetAddressUtils.str(address);
            m_agentConfig = NSClientPeerFactory.getInstance().getAgentConfig(address);
            m_manager = new NsclientManager(m_address);
            m_manager.setPassword(m_agentConfig.getPassword());
            m_manager.setTimeout(m_agentConfig.getTimeout());
            m_manager.setPortNumber(m_agentConfig.getPort());
        }

        public String getAddress() {
            return m_address;
        }

        public NsclientManager getManager() {
            return m_manager;
        }

        public boolean groupIsAvailable(String groupName) {
            NSClientGroupState groupState = m_groupStates.get(groupName);
            if (groupState == null) {
                return false; // If the group availability hasn't been set
                                // yet, it's not available.
            }
            return groupState.isAvailable();
        }

        public void setGroupIsAvailable(String groupName, boolean available) {
            NSClientGroupState groupState = m_groupStates.get(groupName);
            if (groupState == null) {
                groupState = new NSClientGroupState(available);
            }
            groupState.setAvailable(available);
            m_groupStates.put(groupName, groupState);
        }

        public boolean shouldCheckAvailability(String groupName, int recheckInterval) {
            NSClientGroupState groupState = m_groupStates.get(groupName);
            if (groupState == null) {
                // If the group hasn't got a status yet, then it should be
                // checked regardless (and setGroupIsAvailable will
                // be called soon to create the status object)
                return true;
            }
            Date lastchecked = groupState.getLastChecked();
            Date now = new Date();
            return (now.getTime() - lastchecked.getTime() > recheckInterval);
        }

        @SuppressWarnings("unused")
        public void didCheckGroupAvailability(String groupName) {
            NSClientGroupState groupState = m_groupStates.get(groupName);
            if (groupState == null) {
                // Probably an error - log it as a warning, and give up
                LOG.warn("didCheckGroupAvailability called on a group without state - this is odd");
                return;
            }
            groupState.setLastChecked(new Date());
        }

    }

    private static class NSClientGroupState {
        private boolean available = false;
        private Date lastChecked;

        public NSClientGroupState(boolean isAvailable) {
            this(isAvailable, new Date());
        }

        public NSClientGroupState(boolean isAvailable, Date lastChecked) {
            this.available = isAvailable;
            this.lastChecked = lastChecked;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public Date getLastChecked() {
            return lastChecked;
        }

        public void setLastChecked(Date lastChecked) {
            this.lastChecked = lastChecked;
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public RrdRepository getRrdRepository(String collectionName) {
        return NSClientDataCollectionConfigFactory.getInstance().getRrdRepository(collectionName);
    }

}
