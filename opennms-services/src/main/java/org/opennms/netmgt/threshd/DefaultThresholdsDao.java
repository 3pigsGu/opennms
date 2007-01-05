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
package org.opennms.netmgt.threshd;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.config.ThresholdingConfigFactory;
import org.opennms.netmgt.config.threshd.Threshold;

public class DefaultThresholdsDao implements ThresholdsDao {
	
	public DefaultThresholdsDao() {
		
	}

	public ThresholdGroup get(String name) {
		
		File rrdRepository = new File(ThresholdingConfigFactory.getInstance().getRrdRepository(name));

		ThresholdGroup group = new ThresholdGroup(name);
		group.setRrdRepository(rrdRepository);
		
		ThresholdResourceType nodeType = createType(name, "node");
        group.setNodeResourceType(nodeType);
        
        ThresholdResourceType ifType = createType(name, "if");
        group.setIfResourceType(ifType);

        return group;

	}

	private ThresholdResourceType createType(String groupName, String type) {
		ThresholdResourceType resourceType = new ThresholdResourceType(type);
		resourceType.setThresholdMap(createThresholdStateMap(type, groupName));
		return resourceType;
	}

	public Map<String, ThresholdEntity> createThresholdStateMap(String type, String groupName) {
	    Map<String, ThresholdEntity> thresholdMap = new HashMap<String, ThresholdEntity>();
		Iterator iter = ThresholdingConfigFactory.getInstance().getThresholds(groupName).iterator();
	    while (iter.hasNext()) {
	        Threshold thresh = (Threshold) iter.next();
	    
	        // See if map entry already exists for this datasource
	        // If not, create a new one.
	        if (thresh.getDsType().equals(type)) {
	            ThresholdEntity thresholdEntity = thresholdMap.get(thresh.getDsName());
	    
	            // Found entry?
	            if (thresholdEntity == null) {
	                // Nope, create a new one
	                thresholdEntity = new ThresholdEntity();
	                thresholdMap.put(thresh.getDsName(), thresholdEntity);
	            }
	    
	            try {
	                thresholdEntity.setThreshold(thresh);
	            } catch (IllegalStateException e) {
	                log().warn("Encountered duplicate " + thresh.getType() + " for datasource " + thresh.getDsName(), e);
	            }
	    
	        }
	    }
	    return thresholdMap;
	}

	private Category log() {
		return ThreadCategory.getInstance(getClass());
	}

}
