//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2006 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
package org.opennms.netmgt.collectd;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opennms.netmgt.config.DataCollectionConfigFactory;
import org.opennms.netmgt.config.StorageStrategy;
import org.opennms.netmgt.snmp.SnmpInstId;
import org.springframework.orm.ObjectRetrievalFailureException;

public class GenericIndexResourceType extends ResourceType {
	private String m_name;
//	private String m_persistenceSelectorStrategy;
	private StorageStrategy m_storageStrategy;

    private Map<SnmpInstId, GenericIndexResource> m_resourceMap = new HashMap<SnmpInstId, GenericIndexResource>();


	public GenericIndexResourceType(CollectionAgent agent, OnmsSnmpCollection snmpCollection, org.opennms.netmgt.config.datacollection.ResourceType resourceType) {
		super(agent, snmpCollection);
		m_name = resourceType.getName();
                instantiatePersistenceSelectorStrategy(resourceType.getPersistenceSelectorStrategy().getClazz());
                instantiateStorageStrategy(resourceType.getStorageStrategy().getClazz());
        }
        
        private void instantiatePersistenceSelectorStrategy(String className) {
            // TODO write me
        }

        private void instantiateStorageStrategy(String className) {
            Class cinst;
            try {
                cinst = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new ObjectRetrievalFailureException(StorageStrategy.class,
                                                          className,
                                                          "Could not load class",
                                                          e);
            }
            StorageStrategy storageStrategy;
            try {
                storageStrategy = (StorageStrategy) cinst.newInstance();
            } catch (InstantiationException e) {
                throw new ObjectRetrievalFailureException(StorageStrategy.class,
                                                          className,
                                                          "Could not instantiate",
                                                          e);
            } catch (IllegalAccessException e) {
                throw new ObjectRetrievalFailureException(StorageStrategy.class,
                                                          className,
                                                          "Could not instantiate",
                                                          e);
            }
            
            storageStrategy.setResourceTypeName(m_name);

	}

	@Override
	public CollectionResource findResource(SnmpInstId inst) {
		if (!m_resourceMap.containsKey(inst)) {
			m_resourceMap.put(inst, new GenericIndexResource(this, getName(), inst));
		}
		return m_resourceMap.get(inst);
	}

        public CollectionResource findAliasedResource(SnmpInstId inst, String ifAlias) {
        // This is here for completeness but it should not get called from here.
        // findResource should be called instead
            log().debug("findAliasedResource: Should not get called from GenericIndexResourceType");
            return null;
        }

	@Override
	public Collection<AttributeType> getAttributeTypes() {
        return getCollection().getAttributeTypes(getAgent(), DataCollectionConfigFactory.ALL_IF_ATTRIBUTES);
	}

	@Override
	public Collection<GenericIndexResource> getResources() {
		return m_resourceMap.values();
	}

	public String getName() {
		return m_name;
	}
        
        public StorageStrategy getStorageStrategy() {
            return m_storageStrategy;
        }
    
}
