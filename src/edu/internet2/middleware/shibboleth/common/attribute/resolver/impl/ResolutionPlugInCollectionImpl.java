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

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionPlugIn;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionPlugInCollection;

/**
 * Primary implementation of {@link ResolutionPlugInCollection} based on a {@link FastMap}. The following optional
 * Collection methods are not supported:
 * 
 * <ul>
 * <li>{@link Collection#addAll(Collection)</li>
 * <li>{@link Collection#remove(Object)}</li>
 * <li>{@link Collection#removeAll(Collection)</li>
 * <li>{@link Collection#retainAll(Collection)</li>
 * </ul>
 * 
 * In addition, this collection's iterator does not support the {@link Iterator#remove()} method.
 * 
 * TODO: sanity checks: right now, we're checking for ID uniqueness based on the nature of using an internal map. We're
 * currently not checking for any other problems like dependency loops.
 */
public class ResolutionPlugInCollectionImpl<PlugInType extends ResolutionPlugIn> extends AbstractCollection<PlugInType>
        implements ResolutionPlugInCollection<PlugInType>, Serializable {

    private static final long serialVersionUID = -5163392461789339218L;
    
    /** internal container for this collection's elements */
    private Map<String, PlugInType> elementMap;

    /**
     * Constructor
     */
    public ResolutionPlugInCollectionImpl() {
        elementMap = new FastMap<String, PlugInType>();
    }

    /** {@inheritDoc} */
    public PlugInType get(String id) {
        return elementMap.get(id);
    }

    /** {@inheritDoc} */
    public Set<String> getIds() {
        return elementMap.keySet();
    }

    /** {@inheritDoc} */
    public boolean containsId(String id) {
        return elementMap.containsKey(id);
    }

    /** {@inheritDoc} */
    public int size() {
        return elementMap.size();
    }

    /** {@inheritDoc} */
    public boolean add(PlugInType o) {
        if (elementMap.containsKey(o.getId())) {
            throw new IllegalArgumentException("another plug-in already exists with this id.");
        } else {
            elementMap.put(o.getId(), o);
            return true;
        }
    }

    /** Not supported. */
    public boolean addAll(Collection<? extends PlugInType> c) {
        // TODO: try to implement or not? If so, then how we determine success? What if part of the collection is
        // successfully added, but not the rest?
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void clear() {
        elementMap.clear();
    }

    /** {@inheritDoc} */
    public boolean contains(Object o) {
        return elementMap.containsValue(o);
    }

    /** {@inheritDoc} */
    public boolean containsAll(Collection<?> c) {
        return elementMap.values().containsAll(c);
    }

    /** {@inheritDoc} */
    public boolean isEmpty() {
        return elementMap.isEmpty();
    }

    /**
     * returns an unmodifiable {@link Iterator}.
     */
    public Iterator<PlugInType> iterator() {
        return new UnmodifiableIterator<PlugInType>(elementMap.values().iterator());
    }

    /** Not supported. */
    public boolean remove(Object o) {
        // TODO: try to implement or not?
        throw new UnsupportedOperationException();
    }

    /** Not supported. */
    public boolean removeAll(Collection<?> c) {
        // TODO: try to implement or not?
        throw new UnsupportedOperationException();
    }

    /** Not supported. */
    public boolean retainAll(Collection<?> c) {
        // TODO: try to implement or not?
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public Object[] toArray() {
        return elementMap.values().toArray();
    }

    /** {@inheritDoc} */
    public <T> T[] toArray(T[] a) {
        return elementMap.values().toArray(a);
    }

    /**
     * Unmodifiable wrapper for standard {@link Iterator}s that does not support the <code>remove</code> method.
     * 
     * TODO: This should probably be a public class. Better yet, I can't believe {@link Collections} doesn't have a
     * method for it.
     */
    private class UnmodifiableIterator<Type> implements Iterator<Type> {

        /** wrapped iterator */
        Iterator<Type> it;

        /**
         * Constructor
         * 
         * @param it Iterator to wrap
         */
        public UnmodifiableIterator(Iterator<Type> it) {
            this.it = it;
        }

        /** {@inheritDoc} */
        public boolean hasNext() {
            return it.hasNext();
        }

        /** {@inheritDoc} */
        public Type next() {
            return it.next();
        }

        /** Not supported */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}