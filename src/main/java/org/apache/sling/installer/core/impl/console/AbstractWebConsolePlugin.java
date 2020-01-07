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

import java.net.URL;

import javax.servlet.GenericServlet;

@SuppressWarnings("serial")
abstract class AbstractWebConsolePlugin extends GenericServlet {

    /**
     * 
     * @return the prefix under which resources are requested (must not start with "/", must end with "/")
     */
    abstract String getRelativeResourcePrefix();

    /**
     * Method to retrieve static resources from this bundle.
     */
    @SuppressWarnings("unused")
    private URL getResource(final String path) {
        if (path.startsWith("/" + getRelativeResourcePrefix())) {
            // strip label
            int index = path.indexOf('/', 1);
            if (index <= 0) {
                throw new IllegalStateException("The relativeResourcePrefix must contain at least one '/'");
            }
            return this.getClass().getResource(path.substring(index));
        }
        return null;
    }

    /**
     * Copied from org.apache.sling.api.request.ResponseUtil
     * Escape XML text
     * @param input The input text
     * @return The escaped text
     */
    protected String escapeXml(final String input) {
        if (input == null) {
            return null;
        }
    
        final StringBuilder b = new StringBuilder(input.length());
        for(int i = 0;i  < input.length(); i++) {
            final char c = input.charAt(i);
            if(c == '&') {
                b.append("&amp;");
            } else if(c == '<') {
                b.append("&lt;");
            } else if(c == '>') {
                b.append("&gt;");
            } else if(c == '"') {
                b.append("&quot;");
            } else if(c == '\'') {
                b.append("&apos;");
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
}
