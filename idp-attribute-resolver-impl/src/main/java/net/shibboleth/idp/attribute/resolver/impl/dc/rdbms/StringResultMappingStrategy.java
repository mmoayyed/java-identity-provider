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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * A simple {@link ResultMappingStrategy} that assumes all columns in the result set should be mapped and that all
 * values are strings.
 */
public class StringResultMappingStrategy implements ResultMappingStrategy {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StringResultMappingStrategy.class);

    /** {@inheritDoc} */
    @Nonnull public Optional<Map<String, Attribute>> map(@Nonnull final ResultSet results)
            throws ResolutionException {
        Constraint.isNotNull(results, "Result set can not be null");

        try {
            if (!results.next()) {
                log.debug("Result set did not contain any rows, nothing to map");
                return Optional.absent();
            }
            final ResultSetMetaData resultMetadata = results.getMetaData();

            final Map<String, Attribute> attributes = new HashMap<String, Attribute>();

            Attribute attribute;
            for (int i = 1; i <= resultMetadata.getColumnCount(); i++) {
                attribute = new Attribute(resultMetadata.getColumnName(i));
                attribute.getValues().add(new StringAttributeValue(results.getString(i)));
            }

            return Optional.of(attributes);
        } catch (SQLException e) {
            throw new ResolutionException("Error reading data from result set", e);
        }
    }
}