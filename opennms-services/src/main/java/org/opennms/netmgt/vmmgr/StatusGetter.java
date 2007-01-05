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
package org.opennms.netmgt.vmmgr;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatusGetter {
    public static final int STATUS_UNKNOWN = 0;

    public static final int STATUS_RUNNING = 1;

    public static final int STATUS_PARTIALLY_RUNNING = 2;

    public static final int STATUS_NOT_RUNNING = 3;

    public static final int STATUS_CONNECTION_REFUSED = 4;

    private static final String s_defaultInvokeURL = "http://127.0.0.1:8181/"
        + "invoke?objectname=OpenNMS:Name=Manager&operation=status";

    private boolean m_verbose = false;

    private URL m_invokeURL;

    private String m_username;

    private String m_password;

    private int m_status = STATUS_UNKNOWN;

    public StatusGetter() throws MalformedURLException {
        m_invokeURL = new URL(s_defaultInvokeURL);
    }

    public boolean isVerbose() {
        return m_verbose;
    }

    public void setVerbose(boolean verbose) {
        m_verbose = verbose;
    }

    public URL getInvokeURL() {
        return m_invokeURL;
    }

    public void setInvokeURL(URL invokeURL) {
        m_invokeURL = invokeURL;
    }

    public int getStatus() {
        return m_status;
    }

    public static void main(String[] argv) throws Exception {
        StatusGetter statusGetter = new StatusGetter();
        int i;

        for (i = 0; i < argv.length; i++) {
            if (argv[i].equals("-h")) {
                System.out.println("Accepted options:");
                System.out.println("        -v              Verbose mode.");
                System.out.println("        -u <URL>        Alternate invoker URL.");
                System.out.println("The default invoker URL is: "
                        + statusGetter.getInvokeURL());
                statusGetter.setVerbose(true);
            } else if (argv[i].equals("-v")) {
                statusGetter.setVerbose(true);
            } else if (argv[i].equals("-u")) {
                statusGetter.setInvokeURL(new URL(argv[i + 1]));
                i++;
            } else {
                throw new Exception("Invalid command-line option: \""
                        + argv[i] + "\"");
            }
        }

        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("manager",
                                                  "manager".toCharArray());
            }
        });

        statusGetter.queryStatus();

        if (statusGetter.getStatus() == STATUS_NOT_RUNNING
                || statusGetter.getStatus() == STATUS_CONNECTION_REFUSED) {
            System.exit(3); // According to LSB: 3 - service not running
        } else if (statusGetter.getStatus() == STATUS_PARTIALLY_RUNNING) {
            /*
             * According to LSB: reserved for application So, I say 160 -
             * partially running
             */
            System.exit(160);
        } else if (statusGetter.getStatus() == STATUS_RUNNING) {
            System.exit(0); // everything should be good and running
        } else {
            throw new Exception("Unknown status returned from "
                    + "statusGetter.getStatus(): " + statusGetter.getStatus());
        }
    }

    public void queryStatus() throws Exception {

        URLConnection connection = m_invokeURL.openConnection();
        BufferedReader reader;
        try {
            connection.connect();
            reader = new BufferedReader(
                                        new InputStreamReader(
                                                              connection.getInputStream()));
        } catch (ConnectException e) {
            if (isVerbose()) {
                System.out.println("Could not connect to "
                        + getInvokeURL().getHost() + " on port "
                        + getInvokeURL().getPort()
                        + " (OpenNMS might not be running or "
                        + "could be starting up or shutting down): "
                        + e.getMessage());
            }
            m_status = STATUS_CONNECTION_REFUSED;
            return;
        }

        StringBuffer statusResultsBuf = new StringBuffer();
        String line;

        while ((line = reader.readLine()) != null) {
            statusResultsBuf.append(line);
            statusResultsBuf.append("\n");
        }

        String statusResults = statusResultsBuf.toString();

        int i;
        if ((i = statusResults.indexOf("return=\"[")) == -1) {
            throw new Exception("could not find start of status results");
        }
        statusResults = statusResults.substring(i + "return=\"[".length());
        if ((i = statusResults.indexOf("]\"")) == -1) {
            throw new Exception("could not find end of status results");
        }
        statusResults = statusResults.substring(0, i);

        LinkedHashMap results = new LinkedHashMap();
        Pattern p = Pattern.compile("Status: OpenNMS:Name=(\\S+) = (\\S+)");

        /*
         * Once we split a status entry, it will look like this: Status:
         * OpenNMS:Name=Eventd = RUNNING
         */
        while (statusResults.length() > 0) {
            String result;

            i = statusResults.indexOf(", ");

            if (i == -1) {
                result = statusResults;
            } else {
                result = statusResults.substring(0, i);
            }

            Matcher m = p.matcher(result);
            if (!m.matches()) {
                throw new Exception("Result \"" + result
                        + "\" does not match our regular expression");
            }
            results.put(m.group(1), m.group(2));

            if (i == -1) {
                break;
            } else {
                statusResults = statusResults.substring(i + ", ".length());
            }
        }

        /*
         * We want our output to look like this:
         *     OpenNMS.Eventd         : running
         */
        String spaces = "               ";
        int running = 0;
        int services = 0;
        for (Iterator it = results.keySet().iterator(); it.hasNext();) {
            String daemon = (String) it.next();
            String status = ((String) results.get(daemon)).toLowerCase();

            services++;
            if (status.equals("running")) {
                running++;
            }
            if (m_verbose) {
                System.out.println("OpenNMS." + daemon
                        + spaces.substring(daemon.length()) + ": " + status);
            }
        }

        if (services == 0) {
            m_status = STATUS_NOT_RUNNING;
        } else if (running != services) {
            m_status = STATUS_PARTIALLY_RUNNING;
        } else {
            m_status = STATUS_RUNNING;
        }
    }
}
