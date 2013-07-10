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

package net.shibboleth.idp.attribute.filter;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.IdentifiableComponent;

/**
 * Java definition of MatchFunctorType.
 * 
 * Because this can be called in two modes it has two methods.
 * <p>
 * Implementations of this interface <strong>MUST</strong> implement appropriate {@link Object#equals(Object)} and
 * {@link Object#hashCode()} methods.
 * </p>
 */
// TODO - why?
@ThreadSafe
public interface Matcher extends IdentifiableComponent {

    /** A {@link Matcher} that returns true/all attribute values as matched. */
    public static final Matcher MATCHES_ALL = new Matcher() {

        /** {@inheritDoc} */
        public Set<AttributeValue> getMatchingValues(Attribute attribute, AttributeFilterContext filterContext)
                throws AttributeFilterException {
            return Collections.unmodifiableSet(attribute.getValues());
        }

        @Nullable public String getId() {
            // TODO Auto-generated method stub
            return "MATCHES_ALL";
        }
    };

    /** A {@link Matcher} that returns false/no attribute values as matched. */
    public static final Matcher MATCHES_NONE = new Matcher() {

        /** {@inheritDoc} */
        public Set<AttributeValue> getMatchingValues(Attribute attribute, AttributeFilterContext filterContext)
                throws AttributeFilterException {
            return Collections.emptySet();
        }

        @Nullable public String getId() {
            // TODO Auto-generated method stub
            return "MATCHES_NONE";
        }
    };

    /** A {@link Matcher} that fails. targetted primarily at testing, but also at odd corners of parsing. */
    public static final Matcher MATCHER_FAILS = new Matcher() {

        /** {@inheritDoc} */
        public Set<AttributeValue> getMatchingValues(Attribute attribute, AttributeFilterContext filterContext)
                throws AttributeFilterException {
            return null;
        }

        @Nullable public String getId() {
            // TODO Auto-generated method stub
            return "MATCHER_FAILS";
        }
    };
    /**
     * Return those {@link AttributeValues} which match this rule, or null if the matcher failed.
     * 
     * @param attribute the attribute under question.
     * @param filterContext the filter context
     * @return The result of this rule. Null if we failed.
     * @throws AttributeFilterException never. TODO remove this
     */
    @Nullable @NonnullElements @Unmodifiable public Set<AttributeValue> getMatchingValues(
            @Nonnull final Attribute attribute, @Nonnull final AttributeFilterContext filterContext)
            throws AttributeFilterException;

}