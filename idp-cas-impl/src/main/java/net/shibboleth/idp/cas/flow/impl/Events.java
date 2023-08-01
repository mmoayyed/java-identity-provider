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

package net.shibboleth.idp.cas.flow.impl;

import javax.annotation.Nonnull;

import org.springframework.webflow.execution.Event;

/**
 * CAS protocol flow event identifiers.
 *
 * @author Marvin S. Addison
 */
public enum Events {

    /** Successful service ticket validation. */
    ServiceTicketValidated,

    /** Successful proxy ticket validation. */
    ProxyTicketValidated,

    /** Generic success event. */
    Success,

    /** Generic failure event. */
    Failure;


    /**
     * Creates a Spring webflow event whose ID is given by {@link #name()}.
     *
     * @param source Event source.
     *
     * @return Spring webflow event.
     */
    @Nonnull public Event event(@Nonnull final Object source) {
        return new Event(source, name());
    }

}