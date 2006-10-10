package org.opennms.netmgt.dao.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class IndexDao {
    private Map<String, Index> m_nameMap;
    private Map<String, List<Index>> m_tableMap;

    public IndexDao() {
        reset();
    }
    
    public void reset() {
        m_nameMap = new LinkedHashMap<String, Index>();
        m_tableMap = new HashMap<String, List<Index>>();
    }
    
    public void add(Index i) {
        String lowerName = i.getName().toLowerCase();
        if (m_nameMap.containsKey(lowerName)) {
            throw new IllegalArgumentException("Index with name of '"
                                               + lowerName
                                               + "' already exists.");
        }
        
        m_nameMap.put(lowerName, i);
        
        getIndexesForTableCreateIfEmpty(i.getTable().toLowerCase()).add(i);
    }
    
    private List<Index> getIndexesForTableCreateIfEmpty(String table) {
        if (!m_tableMap.containsKey(table)) {
            m_tableMap.put(table, new LinkedList<Index>());
        }
        return m_tableMap.get(table);
    }
    
    public List<Index> getIndexesForTable(String table) {
        String lowerName = table.toLowerCase();
        if (!m_tableMap.containsKey(lowerName)) {
            return new LinkedList<Index>();
        }
        return m_tableMap.get(lowerName);
    }
    
    public Collection<Index> getAllIndexes() {
        return m_nameMap.values();
    }
    
    public void remove(String indexName) {
        String lowerName = indexName.toLowerCase();
        
        Index index = m_nameMap.remove(lowerName);
        if (index == null) {
            throw new IllegalArgumentException("Index with name of '"
                                               + lowerName
                                               + "' does not exist.");
        }
        
        for (List<Index> indexes : m_tableMap.values()) {
            for (Index i : indexes) {
                if (index.equals(i)) {
                    indexes.remove(i);
                    return;
                }
            }
        }
    }
}
