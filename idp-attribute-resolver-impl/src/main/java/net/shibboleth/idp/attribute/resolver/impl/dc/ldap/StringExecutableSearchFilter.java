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

package net.shibboleth.idp.attribute.resolver.impl.dc.ldap;

import javax.annotation.Nonnull;

import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** A simple {@link ExecutableSearchFilter} that executes a static LDAP filter string. */
public class StringExecutableSearchFilter implements ExecutableSearchFilter {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StringExecutableSearchFilter.class);

    /** Search filter to be executed. */
    private final SearchFilter searchFilter;

    /**
     * Constructor.
     * 
     * @param filter search filter to be executed
     */
    public StringExecutableSearchFilter(@Nonnull @NotEmpty final String filter) {
        searchFilter =
                new SearchFilter(Constraint.isNotNull(StringSupport.trimOrNull(filter),
                        "Search filter can not be null or empty"));
    }

    /** {@inheritDoc} */
    public String getResultCacheKey() {
        return String.valueOf(searchFilter.hashCode());
    }

    /** {@inheritDoc} */
    public SearchResult execute(final SearchExecutor executor, final ConnectionFactory factory) throws LdapException {
        final Response<SearchResult> response = executor.search(factory, searchFilter);
        log.trace("Search returned response {}", response);
        return response.getResult();
    }
}