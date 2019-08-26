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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.shibboleth.utilities.java.support.velocity.Template;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.ReferenceInsertionEventHandler;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.VelocityException;
import org.ldaptive.SearchFilter;
import org.ldaptive.auth.AbstractSearchDnResolver;
import org.ldaptive.auth.User;

/**
 * Base class for {@link Template} based search dn resolvers.
 */
public abstract class AbstractTemplateSearchDnResolver extends AbstractSearchDnResolver {

    /** Template. */
    private final Template template;

    /** Event handler used for escaping. */
    private ReferenceInsertionEventHandler eventHandler = new EscapingReferenceInsertionEventHandler();

    /**
     * Creates a new abstract template search DN resolver.
     *
     * @param engine velocity engine
     * @param filter filter template
     *
     * @throws VelocityException if velocity is not configured properly or the filter template is invalid
     */
    public AbstractTemplateSearchDnResolver(final VelocityEngine engine, final String filter) throws VelocityException {
        template = Template.fromTemplate(engine, filter);
        setUserFilter(filter);
    }

    /**
     * Returns the template.
     *
     * @return template
     */
    public Template getTemplate() {
        return template;
    }

    @Override protected SearchFilter createSearchFilter(final User user) {
        final SearchFilter filter = new SearchFilter();
        if (user != null && user.getContext() != null) {
            final VelocityContext context = (VelocityContext) user.getContext();
            final EventCartridge cartridge = new EventCartridge();
            cartridge.addEventHandler(eventHandler);
            cartridge.attachToContext(context);
            final String result = template.merge(context);
            if (result != null && !"".equals(result)) {
                filter.setFilter(result.trim());
            } else {
                logger.error("Invalid template filter produced from {}, cannot be null or empty.", template);
            }
            // keep compatibility with old filter syntax
            if (getUserFilterParameters() != null) {
                filter.setParameters(getUserFilterParameters());
            }
            if (user.getIdentifier() != null && !"".equals(user.getIdentifier())) {
                filter.setParameter("user", user.getIdentifier());
            }
        } else {
            logger.warn("Search filter cannot be created, user input was empty or null");
        }
        return filter;
    }

    /** Escapes LDAP attribute values added to the template context. */
    protected static class EscapingReferenceInsertionEventHandler implements ReferenceInsertionEventHandler {

        @Override public Object referenceInsert(final Context context, final String reference, final Object value) {
            if (value == null) {
                return null;
            } else if (value instanceof Object[]) {
                final List<Object> encodedValues = new ArrayList<>();
                for (final Object o : (Object[]) value) {
                    encodedValues.add(encode(o));
                }
                return encodedValues.toArray();
            } else if (value instanceof Collection<?>) {
                final List<Object> encodedValues = new ArrayList<>();
                for (final Object o : (Collection<?>) value) {
                    encodedValues.add(encode(o));
                }
                return encodedValues;
            } else {
                return encode(value);
            }
        }

        /**
         * Returns {@link SearchFilter#encodeValue} if value is a string or byte array.
         * 
         * @param value to encode
         *
         * @return encoded value
         */
        private Object encode(final Object value) {
            if (value instanceof String){ 
                return SearchFilter.encodeValue((String) value);
            } else if (value instanceof byte[]) {
                return SearchFilter.encodeValue((byte[]) value);
            }
            return value;
        }
    }
}
