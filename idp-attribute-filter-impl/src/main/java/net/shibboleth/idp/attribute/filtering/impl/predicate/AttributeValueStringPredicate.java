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

package net.shibboleth.idp.attribute.filtering.impl.predicate;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

/**
 * Test that an {@link StringAttributeValue} is a match to the supplied parameter.
 */
public class AttributeValueStringPredicate extends BaseStringPredicate implements Predicate {

    /** Logger. */
    private Logger log = LoggerFactory.getLogger(AttributeValueStringPredicate.class);

    /** {@inheritDoc} */
    public boolean apply(@Nullable Object input) {
        
        if (input instanceof StringAttributeValue) {
            final StringAttributeValue stringValue = (StringAttributeValue) input;
            return super.apply(stringValue.getValue());

        } else if (input instanceof AttributeValue) {
            final String valueAsString = ((AttributeValue) input).getValue().toString();
            log.warn("FilterPredicate : Object supplied to StringAttributeValue comparison"
                    + " was of class {}, not StringAttributeValue, comparing with {}", new Object[] {
                    input.getClass().getName(), valueAsString,});
            return super.apply(valueAsString);
        } else {
            log.error("FilterPredicate : Object supplied to StringAttributeValue"
                    + " comparison was of class {}, not AttributeValue", input.getClass().getName());
            return false;
        }
    }

}
