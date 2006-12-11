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

import java.util.Collection;
import java.util.Set;

/**
 * A collection of {@link ResolutionPlugIn}s that extends the base {@link Collection} interface to include a few
 * {@link Map}-like methods for accessing the collection elements. As plug-ins are added to this collection, they are
 * checked for sanity issues like dependency loops and ID uniqueness. This collection is intended to be used internally
 * by the {@link AttributeResolver} to manage resolution plug-ins.
 * 
 * @param <PlugInType> type of plug-in this collection contains
 */
public interface ResolutionPlugInCollection<PlugInType extends ResolutionPlugIn> extends Collection<PlugInType> {

    /**
     * Returns a resolution plug-in from this collection that has the specified id.
     * 
     * @param id id of plug-in to return
     * @return plug-in with the requested id, or <code>null</code> if there no plug-in is found
     */
    public PlugInType get(String id);

    /**
     * Returns a set of the IDs of all plug-ins in this collection.
     * 
     * @return set of ids for the plug-ins in this collection
     */
    public Set<String> getIds();

    /**
     * Returns <code>true</code> if this collection contains a plug-in with the specified id.
     * 
     * @param id plug-in id whose presence in the collection is being checked
     * @return <code>true</code> if this collection contains a plug-in with the specified id
     */
    public boolean containsId(String id);

}