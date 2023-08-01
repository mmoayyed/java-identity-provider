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

package net.shibboleth.idp.profile.context.navigate;

import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.context.MultiRelyingPartyContext;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * A {@link BiFunction} that returns a {@link RelyingPartyContext} based on ID.
 * 
 * <p>If a label is provided, the context will be auto-created if it doesn't already exist.</p>
 */
public class RelyingPartyContextLookupById
        implements BiFunction<MultiRelyingPartyContext,String,RelyingPartyContext> {

    /** Label to use for auto-creation. */
    @Nullable private final String label; 

    /** Constructor. */
    public RelyingPartyContextLookupById() {
        label = null;
    }
    
    /**
     * Constructor.
     * 
     * @param theLabel indicates context should be created if not present, using this label
     */
    public RelyingPartyContextLookupById(@Nonnull @NotEmpty @ParameterName(name="theLabel") final String theLabel) {
        label = Constraint.isNotNull(StringSupport.trimOrNull(theLabel), "Label cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Nullable public RelyingPartyContext apply(@Nullable final MultiRelyingPartyContext input1,
            @Nullable final String input2) {
        if (input1 == null) {
            return null;
        }
        
        final String id = StringSupport.trimOrNull(input2); 
        if (id == null) {
            return null;
        }
        
        RelyingPartyContext rpCtx = input1.getRelyingPartyContextById(id);
        if (rpCtx == null && label != null) {
            rpCtx = new RelyingPartyContext();
            rpCtx.setRelyingPartyId(id);
            assert label != null;
            input1.addRelyingPartyContext(label, rpCtx);
        }
        
        return rpCtx;
    }
    
}