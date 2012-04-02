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

package net.shibboleth.idp.attribute.resolver.impl.dc.rdbms;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.velocity.Template;

/**
 * An {@link ExecutableStatementBuilder} that generates the SQL statement to be executed by evaluating a
 * {@link Template} against the currently resolved attributes within a {@link AttributeResolutionContext}.
 */
public class TemplatedExecutableStatementBuilder implements ExecutableStatementBuilder {

    /** Template evaluated to generate a SQL query. */
    private final Template template;

    /**
     * Constructor.
     * 
     * @param sqlTemplate template evaluated to generate a SQL query
     */
    public TemplatedExecutableStatementBuilder(@Nonnull final Template sqlTemplate) {
        template = Constraint.isNotNull(sqlTemplate, "SQL template can not be null");
    }

    /** {@inheritDoc} */
    public ExecutableStatement build(AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        // TODO Auto-generated method stub
        return null;
    }
}