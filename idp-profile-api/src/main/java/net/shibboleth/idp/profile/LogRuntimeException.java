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

package net.shibboleth.idp.profile;

import javax.annotation.Nonnull;

import org.slf4j.LoggerFactory;

import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * Dedicated bean used to log flow exceptions, to get around issues with Spring Expressions
 * referencing class objects under certain conditions that are so far not understood.
 */
public class LogRuntimeException {
 
    /**
     * Log a message about the caught exception to a particular category.
     * 
     * @param category category to log under
     * @param e exception to log
     */
    public void log(@Nonnull @NotEmpty final String category, @Nonnull final Exception e) {
        LoggerFactory.getLogger(category).error("Uncaught runtime exception",
                e.getCause() != null ? e.getCause() : e);
    }

}