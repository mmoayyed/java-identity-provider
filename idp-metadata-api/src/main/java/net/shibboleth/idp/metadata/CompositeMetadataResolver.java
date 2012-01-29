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

import java.util.Iterator;
import java.util.List;

import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.resolver.Resolver;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.util.criteria.CriteriaSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * A {@link MetadataResolver} implementation that answers requests by composing the answers of child
 * {@link MetadataResolver}s.
 * 
 * @param <MetadataType> type of metadata returned by the resolver
 */
public class CompositeMetadataResolver<MetadataType> extends AbstractIdentifiableInitializableComponent implements
        Resolver<MetadataType, CriteriaSet> {

    /** Resolvers composed by this resolver. */
    private final List<Resolver<MetadataType, CriteriaSet>> resolvers;

    /**
     * Constructor.
     * 
     * @param composedResolvers resolvers composed by this resolver, may be null or contain null elements
     */
    public CompositeMetadataResolver(final List<Resolver<MetadataType, CriteriaSet>> composedResolvers) {
        resolvers =
                ImmutableList.<Resolver<MetadataType, CriteriaSet>> builder()
                        .addAll(Iterables.filter(composedResolvers, Predicates.notNull())).build();
    }

    /** {@inheritDoc} */
    public synchronized void setId(String componentId) {
        super.setId(componentId);
    }

    /**
     * Gets the unmodifiable list of resolvers composed by this resolver.
     * 
     * @return list of resolvers composed by this resolver, never null nor containing null elements
     */
    public List<Resolver<MetadataType, CriteriaSet>> getComposedResolvers() {
        return resolvers;
    }

    /** {@inheritDoc} */
    public Iterable<MetadataType> resolve(CriteriaSet criteria) throws ResolverException {
        return new CompositeMetadataResolverIterable<MetadataType>(resolvers, criteria);
    }

    /** {@inheritDoc} */
    public MetadataType resolveSingle(CriteriaSet criteria) throws ResolverException {
        MetadataType metadata = null;
        for (Resolver<MetadataType, CriteriaSet> resolver : resolvers) {
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
        private final List<Resolver<MetadataType, CriteriaSet>> resolvers;

        /** Criteria being search for. */
        private final CriteriaSet criteria;

        /**
         * Constructor.
         * 
         * @param composedResolvers resolvers from which results will be pulled
         * @param metadataCritiera criteria for the resolver query
         */
        public CompositeMetadataResolverIterable(final List<Resolver<MetadataType, CriteriaSet>> composedResolvers,
                final CriteriaSet metadataCritiera) {
            resolvers =
                    ImmutableList.<Resolver<MetadataType, CriteriaSet>> builder()
                            .addAll(Iterables.filter(composedResolvers, Predicates.notNull())).build();

            // TODO(lajoie) fix this up when CriteriaSet is removed
            criteria = null;
        }

        /** {@inheritDoc} */
        public Iterator<MetadataType> iterator() {
            return new CompositeMetadataResolverIterator();
        }

        /** {@link Iterator} implementation that lazily iterates over each composed resolver. */
        private class CompositeMetadataResolverIterator implements Iterator<MetadataType> {

            /** Iterator over the composed resolvers. */
            private Iterator<Resolver<MetadataType, CriteriaSet>> resolverIterator;

            /** Current resolver from which we are getting results. */
            private Resolver<MetadataType, CriteriaSet> currentResolver;

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
                } catch (ResolverException e) {
                    log.debug("Error encountered attempting to fetch results from resolver", e);
                }
            }
        }
    }
}