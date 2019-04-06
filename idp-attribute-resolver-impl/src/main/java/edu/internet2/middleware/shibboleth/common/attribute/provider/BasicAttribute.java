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

package edu.internet2.middleware.shibboleth.common.attribute.provider;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.ad.impl.ScriptedIdPAttributeImpl;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

/**
 * A class which is here solely to provide compatibility for V2 scripted attribute definitions. The assumption is that a
 * constructor will be called with a string and that only {@link #getValues()} would be called from then on.
 */
public class BasicAttribute extends ScriptedIdPAttributeImpl {


    /**
     * Constructor.
     * 
     * @param id The attribute Id.
     */
    public BasicAttribute(final String id) {
        super(new IdPAttribute(id), "Scripted Attribute Definition: ");
        // Deprecation is NEW in V3.4.4
        DeprecationSupport.warnOnce(ObjectType.CLASS,
                "edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute",
                null, "IdPAttribute");
    }
}
