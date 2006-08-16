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
// Modifications:
//
// 2006 Aug 15: Change log message to be more intuitive, IMHO. - dj@opennms.org
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
// OpenNMS Licensing       <license@opennms.org>
//     http://www.opennms.org/
//     http://www.opennms.com/
//

package org.opennms.netmgt.collectd;

import java.util.Map;

import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.utils.ParameterMap;

public class ServiceParameters {
    
    Map m_parameters;

    public ServiceParameters(Map parameters) {
        m_parameters = parameters;
    }
    
    public Map getParameters() {
        return m_parameters;
    }

    String getDomain() {
        return ParameterMap.getKeyedString(getParameters(), "domain",
        		"default");
    }

    String getStoreByNodeID() {
        return ParameterMap.getKeyedString(getParameters(),
        		"storeByNodeID", "normal");
    }

    String getStoreByIfAlias() {
        return ParameterMap.getKeyedString(getParameters(),
        		"storeByIfAlias", "false");
    }

    String getStorFlagOverride() {
        return ParameterMap.getKeyedString(getParameters(),
        		"storFlagOverride", "false");
    }

    String getIfAliasComment() {
        return ParameterMap.getKeyedString(getParameters(),
        		"ifAliasComment", null);
    }

    boolean aliasesEnabled() {
        return getStoreByIfAlias().equals("true");
    }

    boolean overrideStorageFlag() {
        return !getStorFlagOverride().equals("false");
    }

    void logIfAliasConfig() {
    	log().debug("domain: " + getDomain() + ", "
    			+ "storeByNodeID: " + getStoreByNodeID() + ", "
    			+ "storeByIfAlias: " + getStoreByIfAlias() + ", "
				+ "storFlagOverride: " + getStorFlagOverride() + ", "
				+ "ifAliasComment: " + getIfAliasComment());
    }

    private Category log() {
        return ThreadCategory.getInstance(getClass());
    }

    boolean forceStoreByAlias(String alias) {
        return overrideStorageFlag() && ((alias != null));
    }

    String getCollectionName() {
    	return ParameterMap.getKeyedString(m_parameters, "collection", "default");
    }

    public int getSnmpPort() {
        return ParameterMap.getKeyedInteger(getParameters(), "port", -1);
    }

}
