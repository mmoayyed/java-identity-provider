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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.impl;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionContext;

/**
 * Create a statement from a give template by replacing it's macro's with information within the resolution context.
 */
public class StatementCreator {

    /**
     * Create a statement from a give template by replacing it's macro's with information within the resolution context.
     * 
     * @param template statement template
     * @param resolutionContext the current resolution context
     * 
     * @return constructed statement
     */
    public String createStatement(String template, ResolutionContext resolutionContext) {
        return null;
    }
}