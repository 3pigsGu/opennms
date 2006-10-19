/**
 * 
 */
package org.opennms.report.availability.render;
//
//This file is part of the OpenNMS(R) Application.
//
//OpenNMS(R) is Copyright (C) 2006 The OpenNMS Group, Inc.  All rights reserved.
//OpenNMS(R) is a derivative work, containing both original code, included code and modified
//code that was published under the GNU General Public License. Copyrights for modified 
//and included code are below.
//
//OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
//Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
//For more information contact:
//OpenNMS Licensing       <license@opennms.org>
//  http://www.opennms.org/
//  http://www.opennms.com/
//
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.report.availability.store.ReportStore;
import org.springframework.core.io.Resource;

/**
 * @author jsartin
 * 
 */
public class HTMLReportRenderer implements ReportRenderer {

	private static final String LOG4J_CATEGORY = "OpenNMS.Report";
	
	private ReportStore inputReportStore;
	
	private ReportStore outputReportStore;

	private String inputFileName;
	
	private String outputFileName;
	
	private Resource xsltResource;

	public void render() throws ReportRenderException {

		ThreadCategory.setPrefix(LOG4J_CATEGORY);
		Category log = ThreadCategory.getInstance(HTMLReportRenderer.class);
		try {
			if (log.isInfoEnabled())
                log.info("XSL File " + xsltResource.getFilename());
            
            Reader xsl = new FileReader(xsltResource.getFile());
            
            if (log.isInfoEnabled())
                log.info("input File " + inputFileName);
            
            inputReportStore.setFileName(inputFileName);
            
            Reader xml = new FileReader(inputReportStore.newFile());
            
			if (log.isInfoEnabled())
                log.info("ouput File " + outputFileName);
			
			outputReportStore.setFileName(outputFileName);
			
            FileWriter htmlWriter = new FileWriter(outputReportStore.newFile());
                           
            TransformerFactory tfact = TransformerFactory.newInstance();
            Transformer processor = tfact.newTransformer(new StreamSource(xsl));
            processor.transform(new StreamSource(xml), new StreamResult(htmlWriter));

			htmlWriter.close();
			xsl.close();
			xml.close();

		} catch (IOException ioe) {
			log.fatal("IOException ", ioe);
			throw new ReportRenderException(ioe);
		} catch (TransformerConfigurationException tce) {
			log.fatal("ransformerConfigurationException ", tce);
			throw new ReportRenderException(tce);
		} catch (TransformerException te) {
			log.fatal("TransformerException ", te);
			throw new ReportRenderException(te);
		}
	}

	public void setxsltResource(Resource xsltResource) {
		this.xsltResource = xsltResource;
	}

	public void setInputFileName(String inputFileName) {
		this.inputFileName = inputFileName;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	public void setInputReportStore(ReportStore inputReportStore) {
		this.inputReportStore = inputReportStore;
	}

	public void setOutputReportStore(ReportStore outputReportStore) {
		this.outputReportStore = outputReportStore;
	}
}
