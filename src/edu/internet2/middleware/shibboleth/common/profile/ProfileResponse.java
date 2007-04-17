/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.profile;

/**
 * Wrapper for incoming profile response. Currently serves as a future extension point allowing additional information
 * to be added before the response is provided to the profile handler.
 * 
 * @param <RawResponseType> the type of the raw response encapsulated in the profile response
 */
public interface ProfileResponse<RawResponseType> {

    /**
     * Gets the raw, usually transport specific, response.
     * 
     * @return raw response
     */
    public RawResponseType getRawResponse();

}