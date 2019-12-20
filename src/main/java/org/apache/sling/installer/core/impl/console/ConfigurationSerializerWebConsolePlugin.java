/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.installer.core.impl.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.request.ResponseUtil;
import org.apache.sling.installer.api.serializer.ConfigurationSerializerFactory;
import org.apache.sling.installer.api.serializer.ConfigurationSerializerFactory.Format;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service=javax.servlet.Servlet.class,
    property = {
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        Constants.SERVICE_DESCRIPTION + "=Apache Sling OSGi Installer Configuration Serializer Web Console Plugin",
        "felix.webconsole.label=osgi-installer-config-printer",
        "felix.webconsole.title=OSGi Installer Configuration Printer",
        "felix.webconsole.category=OSGi"
    })
@SuppressWarnings("serial")
public class ConfigurationSerializerWebConsolePlugin extends GenericServlet {

    private static final String PARAMETER_PID = "pid";
    private static final String PARAMETER_FORMAT = "format";
    
    /** The logger */
    private final Logger LOGGER =  LoggerFactory.getLogger(ConfigurationSerializerWebConsolePlugin.class);

    @Reference
    ConfigurationAdmin configurationAdmin;

    @Override
    public void service(final ServletRequest request, final ServletResponse response)
            throws IOException {
        
        final String pid = request.getParameter(PARAMETER_PID);
        final String format = request.getParameter(PARAMETER_FORMAT);
        ConfigurationSerializerFactory.Format serializationFormat = Format.JSON;
        if (StringUtils.isNotBlank(format)) {
            try {
                serializationFormat = ConfigurationSerializerFactory.Format.valueOf(format);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Illegal parameter 'format' given", e);
            }
        }
        final PrintWriter pw = response.getWriter();

        pw.print("<form method='get'>");
        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

        titleHtml(
                pw,
                "OSGi Installer Configuration Printer",
                "To emit the current configuration for a specific OSGi service just enter its PID, select a serialization format and click 'Print'");

        tr(pw);
        tdLabel(pw, "PID");
        tdContent(pw);

        pw.print("<input type='text' name='");
        pw.print(PARAMETER_PID);
        pw.print("' value='");
        if ( pid != null ) {
            pw.print(ResponseUtil.escapeXml(pid));
        }
        
        pw.println("' class='input' size='50'>");
        closeTd(pw);
        closeTr(pw);
        closeTr(pw);

        tr(pw);
        tdLabel(pw, "Serialization Format");
        tdContent(pw);
        // TODO: select current value!
        pw.print("<select name='");
        pw.print(PARAMETER_FORMAT);
        pw.println("'>");
        pw.println("<option value='JSON'>OSGi Configurator JSON</option>");
        pw.println("<option value='CONFIG'>Apache Felix Config</option>");
        pw.println("<option value='PROPERTIES'>Java Properties</option>");
        pw.println("<option value='PROPERTIES_XML'>Java Properties (XML)</option>");
        pw.println("</select>");

        pw.println("&nbsp;&nbsp;<input type='submit' value='Print' class='submit'>");

        closeTd(pw);
        closeTr(pw);

        if (StringUtils.isNotBlank(pid)) {
            tr(pw);
            tdLabel(pw, "Serialized Configuration");
            tdContent(pw);
            
            Configuration configuration = configurationAdmin.getConfiguration(pid);
            Dictionary<String, Object> dictionary = configuration.getProperties();
            if (dictionary == null) {
                pw.println("No configuration for pid '" + pid + "' found!");
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ConfigurationSerializerFactory.create(serializationFormat).serialize(dictionary, baos);
                pw.println("<textarea rows=\"20\" cols=\"120\" readonly>");
                pw.print(new String(baos.toByteArray(), StandardCharsets.UTF_8));
                pw.println("</textarea>");
            }
            closeTd(pw);
            closeTr(pw);
        }

        pw.println("</table>");
        pw.print("</form>");
    }


    private void tdContent(final PrintWriter pw) {
        pw.print("<td class='content' colspan='2'>");
    }

    private void closeTd(final PrintWriter pw) {
        pw.print("</td>");
    }

    @SuppressWarnings("unused")
    private URL getResource(final String path) {
        if (path.startsWith("/servletresolver/res/ui")) {
            return this.getClass().getResource(path.substring(16));
        } else {
            return null;
        }
    }

    private void closeTr(final PrintWriter pw) {
        pw.println("</tr>");
    }

    private void tdLabel(final PrintWriter pw, final String label) {
        pw.print("<td class='content'>");
        pw.print(ResponseUtil.escapeXml(label));
        pw.println("</td>");
    }

    private void tr(final PrintWriter pw) {
        pw.println("<tr class='content'>");
    }

    
    private void titleHtml(final PrintWriter pw, final String title, final String description) {
        tr(pw);
        pw.print("<th colspan='3' class='content container'>");
        pw.print(ResponseUtil.escapeXml(title));
        pw.println("</th>");
        closeTr(pw);

        if (description != null) {
            tr(pw);
            pw.print("<td colspan='3' class='content'>");
            pw.print(ResponseUtil.escapeXml(description));
            pw.println("</th>");
            closeTr(pw);
        }
    }

}
