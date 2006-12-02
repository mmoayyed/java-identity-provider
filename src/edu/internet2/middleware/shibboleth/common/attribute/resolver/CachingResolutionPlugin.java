/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.resolver;

import org.opensaml.xml.util.StorageService;
import org.springframework.context.ApplicationListener;

import edu.internet2.middleware.shibboleth.common.session.LogoutEvent;

/**
 * A resolver plugin capable of caching information it retrieves.
 * 
 * Caching plugins will also receive {@link LogoutEvent}s notifications and may choose to 
 * reclaim cache space when a user logs out.
 */
public interface CachingResolutionPlugin<AttributeType> extends ResolutionPlugIn<AttributeType>, ApplicationListener {

    /**
     * Gets the storage service used to cache information.
     * 
     * @return storage service used to cache information
     */
    public StorageService getStorageService();
    
    /**
     * Sets the storage service used to cache information.
     * 
     * @param storageService storage service used to cache information
     */
    public void setStorageService(StorageService storageService);
    
    /**
     * Returns the time, in milliseconds, to cache the results of the plugin.
     * 
     * @return time, in milliseconds, to cache the results of the plugin
     */
    public long getCacheDuration();
}