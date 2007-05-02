/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.log;

import org.apache.log4j.or.ObjectRenderer;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Renders a {@link AccessLogEntry} as a character seperated string. The format of the string is:
 * 
 * <code>timestamp|remoteHost|serverHost:serverPort|requestPath|</code>
 */
public class CSVAccessEventRenderer implements ObjectRenderer {

    /** Formatter used to convert timestamps to strings. */
    private static DateTimeFormatter dateFormatter = ISODateTimeFormat.basicDateTimeNoMillis();

    /** {@inheritDoc} */
    public String doRender(Object obj) {
        if (!(obj instanceof AccessLogEntry)) {
            return null;
        }

        AccessLogEntry entry = (AccessLogEntry) obj;
        StringBuilder entryString = new StringBuilder();

        entryString.append(entry.getRequestTime().toString(dateFormatter.withZone(DateTimeZone.UTC)));
        entryString.append("|");

        entryString.append(entry.getRemoteHost());
        entryString.append("|");

        entryString.append(entry.getServerHost());
        entryString.append(":");
        entryString.append(entry.getServerPort());
        entryString.append("|");

        entryString.append(entry.getRequestPath());
        entryString.append("|");

        return entryString.toString();
    }
}