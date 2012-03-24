/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.tou;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/** Represents the terms of use. */
@ThreadSafe
public class ToU {

    // TODO: Needs probably an ID too (persisted, compared). For disjunction between different ToUs of relying parties.

    /** Class logger. */
    private final Logger logger = LoggerFactory.getLogger(ToU.class);

    /** The version of the terms of use. */
    private final String version;

    /** The text of the terms of use. */
    private final String text;

    /**
     * Constructs terms of use.
     * 
     * @param touVersion The version of the terms of use.
     * @param resource The resource from where the terms of use text is loaded.
     * @throws TermsOfUseException In case of an initialization error.
     */
    public ToU(final String touVersion, final Resource resource) throws TermsOfUseException {
        version = touVersion;

        final StringBuilder stringBuilder = new StringBuilder();
        try {
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            bufferedReader.close();
        } catch (final IOException e) {
            throw new TermsOfUseException("Error while initializing terms of use", e);
        }
        text = stringBuilder.toString();
        logger.info("ToU version {} initialized from file {}", version, resource);
    }

    /**
     * Gets the text of the terms of use.
     * 
     * @return Returns the text.
     */
    public final String getText() {
        return text;
    }

    /**
     * Gets the version of the terms of use.
     * 
     * @return Returns the version.
     */
    public final String getVersion() {
        return version;
    }
}
