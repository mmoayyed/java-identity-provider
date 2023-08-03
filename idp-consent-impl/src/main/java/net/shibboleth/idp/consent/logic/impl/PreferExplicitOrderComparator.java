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

package net.shibboleth.idp.consent.logic.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Ordering;

import net.shibboleth.shared.annotation.constraint.NullableElements;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Comparator which prefers to order strings according to the order in which they appear in a list, and which falls back
 * to natural ordering for strings not present in the list.
 * 
 * Relies on Guava's {@link Ordering#explicit(List)} to compare strings present in the list. Strings not present in the
 * list are treated as greater than strings present in the list and are compared according to their natural ordering.
 * Does not support comparing null values. Is not serializable, and such should not be used as part of a TreeMap, for
 * example, which is serialized.
 */
public class PreferExplicitOrderComparator implements Comparator<String> {

    /** Explicit ordering. */
    @Nonnull private final Ordering<String> explicitOrdering;

    /** Strings in order. */
    @Nonnull @Unmodifiable private final  List<String> explicitOrder;

    /**
     * Constructor.
     * 
     * @param order the desired order, null and empty strings are ignored, duplicates are removed
     */
    public PreferExplicitOrderComparator(@Nullable @NullableElements final List<String> order) {
        if (order == null) {
            explicitOrder = CollectionSupport.emptyList();
        } else {
            // no duplicates
            explicitOrder = order.
                    stream().
                    map(StringSupport::trimOrNull).
                    filter(e -> e != null).
                    distinct().
                    collect(CollectionSupport.nonnullCollector(Collectors.toUnmodifiableList())).
                    get(); 
        }
        final Ordering<String> ord =Ordering.explicit(explicitOrder);
        assert ord != null;
        explicitOrdering = ord;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException if either argument is null
     */
    public int compare(final String o1, final String o2) {

        final boolean containsLeft = explicitOrder.contains(o1);
        final boolean containsRight = explicitOrder.contains(o2);

        if (containsLeft && containsRight) {
            return explicitOrdering.compare(o1, o2);
        }

        if (containsLeft) {
            return -1;
        }

        if (containsRight) {
            return 1;
        }

        return Ordering.natural().compare(o1, o2);
    }

}