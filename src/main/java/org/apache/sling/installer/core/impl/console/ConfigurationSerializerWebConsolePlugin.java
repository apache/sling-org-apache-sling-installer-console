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
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

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
        "felix.webconsole.label=" + ConfigurationSerializerWebConsolePlugin.LABEL,
        "felix.webconsole.title=OSGi Installer Configuration Printer",
        "felix.webconsole.category=OSGi"
    })
@SuppressWarnings("serial")
public class ConfigurationSerializerWebConsolePlugin extends AbstractWebConsolePlugin {

    public static final String LABEL = "osgi-installer-config-printer";
    private static final String RES_LOC = LABEL + "/res/ui/";
    private static final String PARAMETER_PID = "pid";
    private static final String PARAMETER_FORMAT = "format";

    // copied from org.apache.sling.installer.factories.configuration.impl.ConfigUtil
    /**
     * This property has been used in older versions to keep track where the
     * configuration has been installed from.
     */
    private static final String CONFIG_PATH_KEY = "org.apache.sling.installer.osgi.path";

    /**
     * This property has been used in older versions to keep track of factory
     * configurations.
     */
    private static final String ALIAS_KEY = "org.apache.sling.installer.osgi.factoryaliaspid";

    /** Configuration properties to ignore when printing */
    private static final Set<String> IGNORED_PROPERTIES = new HashSet<>();
    static {
        IGNORED_PROPERTIES.add(Constants.SERVICE_PID);
        IGNORED_PROPERTIES.add(CONFIG_PATH_KEY);
        IGNORED_PROPERTIES.add(ALIAS_KEY);
        IGNORED_PROPERTIES.add(ConfigurationAdmin.SERVICE_FACTORYPID);
    }
    
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
        if (format != null && !format.trim().isEmpty()) {
            try {
                serializationFormat = ConfigurationSerializerFactory.Format.valueOf(format);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Illegal parameter 'format' given, falling back to default '{}'", serializationFormat, e);
            }
        }
        final PrintWriter pw = response.getWriter();

        pw.println("<script type=\"text/javascript\" src=\"" + RES_LOC + "clipboard.js\"></script>");
        pw.print("<form method='get'>");
        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

        titleHtml(
                pw,
                "OSGi Installer Configuration Printer",
                "To emit the configuration properties just enter the configuration PID, select a <a href='https://sling.apache.org/documentation/bundles/configuration-installer-factory.html'>serialization format</a> and click 'Print'");

        tr(pw);
        tdLabel(pw, "PID");
        tdContent(pw);

        pw.print("<input type='text' name='");
        pw.print(PARAMETER_PID);
        pw.print("' value='");
        if ( pid != null ) {
            pw.print(escapeXml(pid));
        }
        
        pw.println("' class='input' size='120'>");
        closeTd(pw);
        closeTr(pw);
        closeTr(pw);

        tr(pw);
        tdLabel(pw, "Serialization Format");
        tdContent(pw);
        pw.print("<select name='");
        pw.print(PARAMETER_FORMAT);
        pw.println("'>");
        option(pw, "JSON", "OSGi Configurator JSON", format);
        option(pw, "CONFIG", "Apache Felix Config", format);
        option(pw, "PROPERTIES", "Java Properties", format);
        option(pw, "PROPERTIES_XML", "Java Properties (XML)", format);
        pw.println("</select>");

        pw.println("&nbsp;&nbsp;<input type='submit' value='Print' class='submit'>");

        closeTd(pw);
        closeTr(pw);

        if (pid != null && !pid.trim().isEmpty()) {
            tr(pw);
            tdLabel(pw, "Serialized Configuration Properties");
            tdContent(pw);
            
            Configuration configuration = configurationAdmin.getConfiguration(pid, null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties == null) {
                pw.print("<p class='ui-state-error-text'>");
                pw.print("No configuration properties for pid '" + escapeXml(pid) + "' found!");
                pw.println("</p>");
            } else {
                properties = cleanConfiguration(properties);
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ConfigurationSerializerFactory.create(serializationFormat).serialize(properties, baos);
                    pw.println("<textarea rows=\"20\" cols=\"120\" id=\"output\" readonly>");
                    pw.print(new String(baos.toByteArray(), StandardCharsets.UTF_8));
                    pw.println("</textarea>");
                    pw.println("<button type='button' id='copy'>Copy to Clipboard</a>");
                } catch (Throwable e) {
                    pw.print("<p class='ui-state-error-text'>");
                    pw.print("Error serializing pid '" + escapeXml(pid) + "': " + e.getMessage());
                    pw.println("</p>");
                    LOGGER.warn("Error serializing pid '{}'", pid, e);
                }
            }
            closeTd(pw);
            closeTr(pw);
        }

        pw.println("</table>");
        pw.print("</form>");
    }

    // copied from org.apache.sling.installer.factories.configuration.impl.ConfigUtil
    /**
     * Remove all ignored properties
     */
    public static Dictionary<String, Object> cleanConfiguration(final Dictionary<String, Object> config) {
        final Dictionary<String, Object> cleanedConfig = new Hashtable<>();
        final Enumeration<String> e = config.keys();
        while(e.hasMoreElements()) {
            final String key = e.nextElement();
            if ( !IGNORED_PROPERTIES.contains(key) ) {
                cleanedConfig.put(key, config.get(key));
            }
        }

        return cleanedConfig;
    }

    private void tdContent(final PrintWriter pw) {
        pw.print("<td class='content' colspan='2'>");
    }

    private void closeTd(final PrintWriter pw) {
        pw.print("</td>");
    }

    private void closeTr(final PrintWriter pw) {
        pw.println("</tr>");
    }

    private void tdLabel(final PrintWriter pw, final String label) {
        pw.print("<td class='content'>");
        pw.print(label);
        pw.println("</td>");
    }

    private void tr(final PrintWriter pw) {
        pw.println("<tr class='content'>");
    }

    private void option(final PrintWriter pw, String value, String label, String selectedValue) {
        pw.print("<option value='");
        pw.print(value);
        pw.print("'");
        if (value.equals(selectedValue)) {
            pw.print(" selected");
        }
        pw.print(">");
        pw.print(label);
        pw.println("</option>");
    }

    private void titleHtml(final PrintWriter pw, final String title, final String description) {
        tr(pw);
        pw.print("<th colspan='3' class='content container'>");
        pw.print(escapeXml(title));
        pw.println("</th>");
        closeTr(pw);

        if (description != null) {
            tr(pw);
            pw.print("<td colspan='3' class='content'>");
            pw.print(description);
            pw.println("</th>");
            closeTr(pw);
        }
    }

    @Override
    String getRelativeResourcePrefix() {
        return RES_LOC;
    }

}
