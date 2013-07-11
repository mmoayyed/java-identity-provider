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

package net.shibboleth.idp.attribute.filter.impl.policyrule.saml.attributemapper;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;

import org.opensaml.core.xml.XMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder for {@link XMLObject} into {@link XMLObjectAttributeValue}.
 */
public class XmlObjectAttributeValueMapper extends BaseAttributeValueMapper {

    /** logger . */
    private Logger log = LoggerFactory.getLogger(XmlObjectAttributeValueMapper.class);
    
    /** {@inheritDoc} */
    @Nullable protected AttributeValue decodeValue(final XMLObject object)  {
        final List<XMLObject> children = object.getOrderedChildren();
        
        if (null == children || children.isEmpty()) {
            log.debug("{} attribute had no XML children, ignored", getLogPrefix());
            return null;
        }
        if (children.size() > 1) {
            log.debug("{} attribute has more than one child, returning first value only", getLogPrefix());
        }
        final XMLObject child = children.get(0);
        log.debug("{} returning value of type {}", getLogPrefix(), child.getClass().toString());
        return new XMLObjectAttributeValue(child);
    }

    /** {@inheritDoc} */
    @Nonnull protected String getAttributeTypeName() {
        return "XMLObject";
    }

}
