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

package net.shibboleth.idp.ui.context;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * The class contains the mechanisms to manipulate language
 * sensitive data about the relying party.  The data is looked 
 * up via an injected function.
 * @param <T> the type which this stores data for.
 */
@NotThreadSafe
public class RelyingPartyUIData<T> {
    
    /** logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RelyingPartyUIInformation.class);

    /** How to get from the generic type RP to the list of supported 
     * information of this class.
     */
    @Nullable private Function<T, Map<String, String>> dataStrategy;
    
    /** The map of languages to service names from the RP.*/
    @Nonnull private Map<String, String> dataMap = Collections.EMPTY_MAP;
    
    /** The string to return when we cannot find a service name. */
    @Nonnull private String defaultData;

    /** Gets the mechanism to get the data.
     * @return the mechanism.
     */
    @Nullable public Function<T, Map<String, String>> getDataStrategy() {
        return dataStrategy;
    }

    /** Sets the mechanism to get the data.
     * @param strategy what set.
     */
    public void setDataStrategy(@Nullable Function<T, Map<String, String>> strategy) {
        dataStrategy = strategy;
    }
    
    /** Get the string to return when we cannot find a language match.
     * @return Returns the name
     */
    @Nonnull public String getDefaultData() {
        return defaultData;
    }

    /** Set the string to return when we cannot find a language match.
     * @param data what to set.
     */
    public void setDefaultData(@Nullable  String data) {
        if (null == data) {
            log.debug("Value for {} was null, value not set", defaultData);
        } else {
            defaultData = data;
        }
    }

    /** Get the map of languages to service names from the RP.
     * @return the map
     * */
    @Nonnull public Map<String, String> getDataMap() {
        return dataMap;
    }
    
    /** Set up the default map from the relying party.  Then add the default value
     * @param relyingParty the relyingParty to interrogate
     */
    public void setRelyingParty(T relyingParty) {
        if (null != getDataStrategy()) {
            dataMap = getDataStrategy().apply(relyingParty);
        }
        if (null == dataMap || dataMap.isEmpty()) {
            dataMap = Collections.singletonMap((String) null, defaultData);
        } else {
            if (null == dataMap.get(null)) {
                dataMap.put(null, defaultData);
            }
        }
    }
}
