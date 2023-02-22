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

package net.shibboleth.idp.authn.duo.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Duo device, intended for use with a jackson
 * {@link com.fasterxml.jackson.databind.ObjectMapper}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DuoDevice {

    /** the Duo device identifier. */
    @JsonProperty("device") @Nullable private String device;

    /** the Duo device type. */
    @JsonProperty("type") @Nullable private String type;

    /** the Duo device number. */
    @JsonProperty("number") @Nullable private String number;

    /** the Duo device name. */
    @JsonProperty("name") @Nullable private String name;

    /** a {@link List} of Duo device capabilities. */
    @JsonProperty("capabilities") @Nullable private List<String> capabilities;

    /** Constructor. */
    public DuoDevice() {
        capabilities = new ArrayList<>();
    }

    /**
     * Get the device identifier.
     * 
     * @return the device identifier
     */
    @Nonnull public String getDevice() {
        assert device != null;
        return device;
    }

    /**
     * Get the device type.
     * 
     * @return the device type
     */
    @Nonnull public String getType() {
        assert type != null;
        return type;
    }

    /**
     * Get the device number.
     * 
     * @return the device number
     */
    @Nonnull public String getNumber() {
        assert number != null;
        return number;
    }

    /**
     * Get the device name.
     * 
     * @return the device name
     */
    @Nonnull public String getName() {
        assert name != null;
        return name;
    }

    /**
     * Get the device capabilities.
     * 
     * @return the device capabilities
     */
    @Nonnull public Collection<String> getCapabilities() {
        assert capabilities != null;
        return capabilities;
    }

}