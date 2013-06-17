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

package net.shibboleth.idp.attribute.filter.impl.saml.attributemapper;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSBase64Binary;
import org.opensaml.core.xml.schema.XSBoolean;
import org.opensaml.core.xml.schema.XSDateTime;
import org.opensaml.core.xml.schema.XSInteger;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.XSURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base of the classes that map SAML2 attribute values into IdP attribute values.
 * 
 */
public abstract class BaseAttributeValueMapper extends AbstractInitializableComponent {
    
    /** logger. */
    private final Logger log = LoggerFactory.getLogger(BaseAttributeValueMapper.class);

    /** The String used to prefix log message. */
    private String logPrefix;

    /**
     * Convert from a list of the SAML Attributes into the IDP Attributes.
     * 
     * @param inputs the list of SAML Attributes
     * @return a list of IdP Attributes
     */
    @Nonnull public List<AttributeValue> decodeValues(List<XMLObject> inputs) {
        List<AttributeValue> outputs = new ArrayList<AttributeValue>(inputs.size());

        for (XMLObject input : inputs) {
            AttributeValue output = decodeValue(input);
            if (null != output) {
                outputs.add(output);
            }
        }
        return outputs;

    }

    /**
     * Function to return the string contents if this is the correct type.
     * 
     * @param object The object to inspect.
     * @return Its contents suitably decoded. Returns null if we could not decode.
     */
    @Nullable protected String getStringValue(final XMLObject object) {
        String retVal = null;
        
        if (object instanceof XSString) {
            
            retVal = ((XSString) object).getValue();
            
        } else if (object instanceof XSURI) {
            
            retVal =  ((XSURI) object).getValue();
            
        } else if (object instanceof XSBoolean) {
            
            retVal =  ((XSBoolean) object).getValue().getValue() ? "1" : "0";
            
        } else if (object instanceof XSInteger) {
            
            retVal =  ((XSInteger) object).getValue().toString();
            
        } else {
            
            retVal = decodeComplex(object);
        }

        if (null == retVal) {
            log.info("{} value of type {} could not be converted", getLogPrefix(), object.getClass().toString());
        }
        return retVal;
    }

    /**
     * Helper function for decodeValue.  Only here because checkstyle needs it.
     * @param object the object to look at
     * @return a string, if conversion posible, null otherwise
     */
    @Nullable private String decodeComplex(XMLObject object) {
        String retVal = null;
        if (object instanceof XSDateTime) {
            
            final DateTime dt = ((XSDateTime) object).getValue();
            if (dt != null) {
                retVal =  ((XSDateTime) object).getDateTimeFormatter().print(dt);
            } else {
                retVal = null;
            }
            
        } else  if (object instanceof XSBase64Binary) {
            
            retVal =  ((XSBase64Binary) object).getValue();
            
        } else if (object instanceof XSAny) {
            
            final XSAny wc = (XSAny) object;
            if (wc.getUnknownAttributes().isEmpty() && wc.getUnknownXMLObjects().isEmpty()) {
                retVal =  wc.getTextContent();
            } else { 
                retVal =  null;
            }
        }
        return retVal;
    }

    /**
     * Return a string which is to be prepended to all log messages. This is set by the enclosing AttributeMapper
     * 
     * @return Returns the logPrefix.
     */
    public String getLogPrefix() {
        return logPrefix;
    }

    /**
     * Set the log prefix to use.
     * 
     * @param prefix The logPrefix to set.
     */
    public void setLogPrefix(String prefix) {
        logPrefix = prefix;
    }

    /**
     * Function to decode a single {@link XMLObject} into an {@link AttributeValue}.
     * 
     * @param object the object to decode
     * @return the returned final {@link AttributeValue} or null if decoding failed
     */
    @Nullable protected abstract AttributeValue decodeValue(final XMLObject object);

    /**
     * Return the output type (for logging purposes).
     * 
     * @return the output type.
     */
    @Nonnull protected abstract String getAttributeTypeName();

}
