/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.cas.protocol;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Abstract base class for protocol response messages.
 *
 * @author Marvin S. Addison
 * @since 3.2.0
 */
public class AbstractProtocolResponse {

    /** CAS protocol error code populated on failure. */
    @Nullable private String errorCode;

    /** CAS protocol error detail populated on failure. */
    @Nullable private String errorDetail;

    /**
     * Get the non-null error code on a ticket validation failure condition.
     * 
     * @return non-null error code on a ticket validation failure condition
     */
    @Nullable public String getErrorCode() {
        return errorCode;
    }

    /**
     * Set the non-null error code on a ticket validation failure condition.
     * 
     * @param code non-null error code on a ticket validation failure condition
     */
    public void setErrorCode(@Nonnull final String code) {
        errorCode = code;
    }

    /**
     * Get the non-null error detail on a ticket validation failure condition.
     * 
     * @return non-null error detail on a ticket validation failure condition
     */
    @Nullable public String getErrorDetail() {
        return errorDetail;
    }

    /**
     * Set the non-null error detail on a ticket validation failure condition.
     * 
     * @param code non-null error detail on a ticket validation failure condition
     */
    public void setErrorDetail(@Nonnull final String code) {
        errorDetail = code;
    }

}