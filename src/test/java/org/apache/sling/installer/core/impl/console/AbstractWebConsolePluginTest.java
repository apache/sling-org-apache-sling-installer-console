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

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
class AbstractWebConsolePluginTest {
    private AbstractWebConsolePlugin plugin = new AbstractWebConsolePlugin() {

        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        String getRelativeResourcePrefix() {
            return "osgi-installer/res/ui/";
        }
    };

    /**
     * Test method for {@link org.apache.sling.installer.core.impl.console.AbstractWebConsolePlugin#escapeXml(java.lang.String)}.
     */
    @Test
    void testEscapeXml() {
        assertNull(plugin.escapeXml(null));
        assertEquals("", plugin.escapeXml(""));
        assertEquals("&lt;hello/&gt;", plugin.escapeXml("<hello/>"));
        assertEquals("&quot;dog&apos;s &amp; cat&quot;", plugin.escapeXml("\"dog's & cat\""));
    }

    /**
     * Test method for {@link
     * org.apache.sling.installer.factories.configuration.impl.ConfigurationSerializerWebConsolePlugin#getResource(java.lang.String)}.
     */
    @Test
    void testGetResource() throws SecurityException, IllegalArgumentException {
        final URL value1 = ReflectionTools.invokeMethodWithReflection(
                plugin, "getResource", new Class[] {String.class}, URL.class, new Object[] {"/invalid"});
        assertNull(value1);

        final URL value2 = ReflectionTools.invokeMethodWithReflection(
                plugin, "getResource", new Class[] {String.class}, URL.class, new Object[] {
                    "/osgi-installer/res/ui/list.css"
                });
        assertNotNull(value2);

        final URL value3 = ReflectionTools.invokeMethodWithReflection(
                plugin, "getResource", new Class[] {String.class}, URL.class, new Object[] {
                    "/osgi-installer/res/ui/invalid.css"
                });
        assertNull(value3);
    }
}
