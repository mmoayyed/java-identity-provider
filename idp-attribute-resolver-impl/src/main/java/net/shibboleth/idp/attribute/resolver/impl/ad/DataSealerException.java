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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import javax.annotation.Nullable;

//TODO This needs to find a better home 

/** Exceptions during use of the {@link DataSealer}. */
public class DataSealerException extends Exception {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 5366134892878709189L;

    /**
     * Constructor.
     */
    public DataSealerException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     */
    public DataSealerException(@Nullable final String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param wrappedException exception to be wrapped by this one
     */
    public DataSealerException(@Nullable final Exception wrappedException) {
        super(wrappedException);
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     * @param wrappedException exception to be wrapped by this one
     */
    public DataSealerException(@Nullable final String message, @Nullable final Exception wrappedException) {
        super(message, wrappedException);
    }
}