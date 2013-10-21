/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.rrdtool;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.rrd.model.v3.CFType;
import org.opennms.netmgt.rrd.model.v3.DSType;
import org.opennms.netmgt.rrd.model.v3.RRDv3;
import org.opennms.netmgt.rrd.model.v3.Xport;

/**
 * The Class RRD Parsing Test.
 * 
 * @author Alejandro Galue <agalue@opennms.org>
 */
public class RrdParsingTest {

    /**
     * Parses a simple RRD.
     *
     * @throws Exception the exception
     */
    @Test
    public void parseRrdSimple() throws Exception {
        RRDv3 rrd = JaxbUtils.unmarshal(RRDv3.class, new File("src/test/resources/rrd-dump.xml"));
        Assert.assertNotNull(rrd);
        Assert.assertEquals(new Long(300), rrd.getStep());
        Assert.assertEquals(new Long(1233926670), rrd.getLastUpdate());
        Assert.assertEquals("ifInDiscards", rrd.getDataSources().get(0).getName());
        Assert.assertEquals(DSType.COUNTER, rrd.getDataSources().get(0).getType());
        Assert.assertEquals(new Integer(0), rrd.getDataSources().get(0).getUnknownSec());

        Assert.assertEquals(CFType.AVERAGE, rrd.getRras().get(0).getConsolidationFunction());
        Assert.assertEquals(new Integer(1), rrd.getRras().get(0).getPdpPerRow());

        Assert.assertEquals(new Integer(1), rrd.getRras().get(0).getPdpPerRow());
        Assert.assertEquals(new Long(1233321900), rrd.getStartTimestamp(rrd.getRras().get(0)));
        Assert.assertEquals(new Integer(12), rrd.getRras().get(1).getPdpPerRow());
        Assert.assertEquals(new Long(1228572000), rrd.getStartTimestamp(rrd.getRras().get(1)));
        Assert.assertEquals(new Integer(288), rrd.getRras().get(4).getPdpPerRow());
        Assert.assertEquals(new Long(1202342400), rrd.getStartTimestamp(rrd.getRras().get(4)));
    }

    /**
     * Parses the RRD with computed DS.
     *
     * @throws Exception the exception
     */
    @Test
    public void parseRrdWithComputedDs() throws Exception {
        RRDv3 rrd = JaxbUtils.unmarshal(RRDv3.class, new File("src/test/resources/rrd-dump-compute-ds.xml"));
        Assert.assertNotNull(rrd);
    }

    /**
     * Parses the RRD with aberrant behavior detection.
     *
     * @throws Exception the exception
     */
    @Test
    public void parseRrdWithAberrantBehaviorDetection() throws Exception {
        RRDv3 rrd = JaxbUtils.unmarshal(RRDv3.class, new File("src/test/resources/rrd-dump-aberrant-behavior-detection.xml"));
        Assert.assertNotNull(rrd);
    }

    /**
     * Parses the Xport.
     *
     * @throws Exception the exception
     */
    @Test
    public void parseXport() throws Exception {
        Xport xport = JaxbUtils.unmarshal(Xport.class, new File("src/test/resources/rrd-xport.xml"));
        Assert.assertNotNull(xport);
        Assert.assertEquals(new Integer(300), xport.getMeta().getStep());
        Assert.assertEquals(new Long(1206312900), xport.getMeta().getStart());
        Assert.assertEquals(new Long(1206316500), xport.getMeta().getEnd());
        Assert.assertEquals("load average 5min", xport.getMeta().getLegends().get(0));
        Assert.assertEquals(new Long(1206312900), xport.getRows().get(0).getTimestamp());
        Assert.assertEquals(new Double(19.86), xport.getRows().get(0).getValues().get(0));
    }
}
