package org.opennms.web.performance;

import java.util.List;

public interface GraphResourceType {
    public String getName();
    public String getLabel();
    public boolean isResourceTypeOnNode(int nodeId);
    public List<GraphResource> getResourcesForNode(int nodeId);
    public String getRelativePathForAttribute(String resourceParent, String resource, String attribute);
    public boolean isResourceTypeOnDomain(String domain);
    public List<GraphResource> getResourcesForDomain(String domain);
}
