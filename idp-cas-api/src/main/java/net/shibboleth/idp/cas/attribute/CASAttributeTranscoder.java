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

package net.shibboleth.idp.cas.attribute;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * Marker interface for transcoders that support CAS attributes.
 */
public interface CASAttributeTranscoder extends AttributeTranscoder<Attribute> {

    /** The attribute name. */
    @Nonnull @NotEmpty static final String PROP_NAME = "cas.name";

}