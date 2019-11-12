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

package net.shibboleth.idp.attribute.resolver.dc.rdbms.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * An {@link net.shibboleth.idp.attribute.resolver.dc.ExecutableSearchBuilder}. It generates the SQL statement to
 * be executed by invoking {@link String#format(String, Object...)} with
 * {@link AttributeResolutionContext#getPrincipal() }.
 */
public class FormatExecutableStatementBuilder extends AbstractExecutableStatementBuilder {

    /** SQL query string. */
    @NonnullAfterInit private String sqlQuery;
    
    /** Set the query to search the database. 
     * @param query query to search the database
     */
    public void setQuery(@Nonnull final String query) {
        sqlQuery = Constraint.isNotNull(query, "SQL query cannot be null");        
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (null == sqlQuery) {
            throw new  ComponentInitializationException(
                    "FormatExecutableStatementBuilder: SQL query cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override protected String getSQLQuery(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Map<String, List<IdPAttributeValue>> dependencyAttributes) {
        final List<Object> args = new ArrayList<>();
        if (dependencyAttributes != null && !dependencyAttributes.isEmpty()) {
            for (final Map.Entry<String, List<IdPAttributeValue>> entry : dependencyAttributes.entrySet()) {
                for (final IdPAttributeValue value : entry.getValue()) {
                    if (value.getNativeValue() instanceof String){ 
                        args.add(((String) value.getNativeValue()).replace("'", "''"));
                    } else {
                        args.add(value.getNativeValue());
                    }
                }
            }
        } else {
            if (resolutionContext.getPrincipal() != null) {
                args.add(resolutionContext.getPrincipal().replace("'", "''"));
            } else {
                args.add(null);
            }
        }
        return String.format(sqlQuery, args.toArray());
    }

}
