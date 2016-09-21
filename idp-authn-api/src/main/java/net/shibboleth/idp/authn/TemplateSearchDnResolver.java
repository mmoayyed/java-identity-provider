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

package net.shibboleth.idp.authn;

import java.util.Arrays;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.ConnectionFactoryManager;
import org.ldaptive.LdapException;

/**
 * {@link Template} based search dn resolver.
 */
public class TemplateSearchDnResolver extends AbstractTemplateSearchDnResolver implements ConnectionFactoryManager {

    /** Connection factory. */
    private ConnectionFactory factory;

    /**
     * Creates a new template search DN resolver.
     *
     * @param engine velocity engine
     * @param filter filter template
     *
     * @throws VelocityException if velocity is not configured properly or the filter template is invalid
     */
    public TemplateSearchDnResolver(final VelocityEngine engine, final String filter) throws VelocityException {
        super(engine, filter);
    }

    /**
     * Creates a new template search DN resolver.
     *
     * @param cf connection factory
     * @param engine velocity engine
     * @param filter filter template
     *
     * @throws VelocityException if velocity is not configured properly or the filter template is invalid
     */
    public TemplateSearchDnResolver(final ConnectionFactory cf, final VelocityEngine engine, final String filter)
            throws VelocityException {
        super(engine, filter);
        setConnectionFactory(cf);
    }

    @Override public ConnectionFactory getConnectionFactory() {
        return factory;
    }

    @Override public void setConnectionFactory(final ConnectionFactory cf) {
        factory = cf;
    }

    @Override protected Connection getConnection() throws LdapException {
        final Connection conn = factory.getConnection();
        conn.open();
        return conn;
    }

    @Override public String toString() {
        return String.format(
                "[%s@%d::factory=%s, templateName=%s, baseDn=%s, userFilter=%s, userFilterParameters=%s, "
                        + "allowMultipleDns=%s, subtreeSearch=%s, derefAliases=%s, followReferrals=%s]",
                getClass().getName(), hashCode(), factory, getTemplate().getTemplateName(), getBaseDn(),
                getUserFilter(), Arrays.toString(getUserFilterParameters()), getAllowMultipleDns(), getSubtreeSearch(),
                getDerefAliases(), getFollowReferrals());
    }
}
