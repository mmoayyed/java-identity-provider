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

package net.shibboleth.idp.metadata;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.shibboleth.idp.AbstractComponent;

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.collections.LazyList;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MetadataResolver} implementation that answers requests by composing the answers of child
 * {@link MetadataResolver}s.
 * 
 * @param <MetadataType> type of metadata returned by the resolver
 */
public class CompositeMetadataResolver<MetadataType> extends AbstractComponent implements
        MetadataResolver<MetadataType> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(CompositeMetadataResolver.class);

    /** Resolvers composed by this resolver. */
    private final List<MetadataResolver<MetadataType>> resolvers;

    /**
     * Constructor.
     * 
     * @param id unique identifier for this resolver
     * @param composedResolvers resolvers composed by this resolver, may be null or contain null elements
     */
    public CompositeMetadataResolver(final String id, final List<MetadataResolver<MetadataType>> composedResolvers) {
        super(id);

        if (composedResolvers == null || composedResolvers.isEmpty()) {
            resolvers = Collections.emptyList();
        } else {
            LazyList<MetadataResolver<MetadataType>> checkedResolvers =
                    CollectionSupport.addNonNull(composedResolvers, new LazyList<MetadataResolver<MetadataType>>());
            resolvers = Collections.unmodifiableList(checkedResolvers);
        }
    }

    /**
     * Gets the unmodifiable list of resolvers composed by this resolver.
     * 
     * @return list of resolvers composed by this resolver, never null nor containing null elements
     */
    public List<MetadataResolver<MetadataType>> getComposedResolvers() {
        return resolvers;
    }

    /** {@inheritDoc} */
    public Iterable<MetadataType> resolve(CriteriaSet criteria) throws SecurityException {
        return new CompositeMetadataResolverIterable<MetadataType>(resolvers, criteria);
    }

    /** {@inheritDoc} */
    public MetadataType resolveSingle(CriteriaSet criteria) throws SecurityException {
        MetadataType metadata = null;
        for (MetadataResolver<MetadataType> resolver : resolvers) {
            metadata = resolver.resolveSingle(criteria);
            if (metadata != null) {
                return metadata;
            }
        }

        return null;
    }

    /**
     * {@link Iterable} implementation that provides an {@link Iterator} that lazily iterates over each composed
     * resolver.
     */
    private static class CompositeMetadataResolverIterable<MetadataType> implements Iterable<MetadataType> {

        /** Class logger. */
        private final Logger log = LoggerFactory.getLogger(CompositeMetadataResolverIterable.class);

        /** Resolvers over which to iterate. */
        private final List<MetadataResolver<MetadataType>> resolvers;

        /** Criteria being search for. */
        private final CriteriaSet criteria;

        /**
         * Constructor.
         * 
         * @param composedResolvers resolvers from which results will be pulled
         * @param metadataCritiera criteria for the resolver query
         */
        public CompositeMetadataResolverIterable(final List<MetadataResolver<MetadataType>> composedResolvers,
                final CriteriaSet metadataCritiera) {
            if (composedResolvers == null || composedResolvers.isEmpty()) {
                resolvers = Collections.emptyList();
            } else {
                LazyList<MetadataResolver<MetadataType>> checkedResolvers =
                        CollectionSupport.addNonNull(composedResolvers, new LazyList<MetadataResolver<MetadataType>>());
                resolvers = Collections.unmodifiableList(checkedResolvers);
            }

            criteria = CollectionSupport.addNonNull(metadataCritiera, new CriteriaSet());

        }

        /** {@inheritDoc} */
        public Iterator<MetadataType> iterator() {
            return new CompositeMetadataResolverIterator();
        }

        /** {@link Iterator} implementation that lazily iterates over each composed resolver. */
        private class CompositeMetadataResolverIterator implements Iterator<MetadataType> {

            /** Iterator over the composed resolvers. */
            private Iterator<MetadataResolver<MetadataType>> resolverIterator;

            /** Current resolver from which we are getting results. */
            private MetadataResolver<MetadataType> currentResolver;

            /** Iterator over the results of the current resolver. */
            private Iterator<MetadataType> currentResolverMetadataIterator;

            /** Constructor. */
            public CompositeMetadataResolverIterator() {
                resolverIterator = resolvers.iterator();
            }

            /** {@inheritDoc} */
            public boolean hasNext() {
                if (!currentResolverMetadataIterator.hasNext()) {
                    proceedToNextResolverIterator();
                }

                return currentResolverMetadataIterator.hasNext();
            }

            /** {@inheritDoc} */
            public MetadataType next() {
                if (!currentResolverMetadataIterator.hasNext()) {
                    proceedToNextResolverIterator();
                }

                return currentResolverMetadataIterator.next();
            }

            /** {@inheritDoc} */
            public void remove() {
                throw new UnsupportedOperationException();
            }

            /**
             * Proceed to the next composed resolvers that has a response to the resolution query.
             */
            private void proceedToNextResolverIterator() {
                try {
                    while (resolverIterator.hasNext()) {
                        currentResolver = resolverIterator.next();
                        currentResolverMetadataIterator = currentResolver.resolve(criteria).iterator();
                        if (currentResolverMetadataIterator.hasNext()) {
                            return;
                        }
                    }
                } catch (SecurityException e) {
                    log.debug("Error encountered attempting to fetch results from resolver {}",
                            currentResolver.getId(), e);
                }
            }
        }
    }
}