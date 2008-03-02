/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.saml;

import org.opensaml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.basic.AbstractMatchFunctor;

/**
 * Base class for match functors that check if a given entity is in an entity group.
 */
public abstract class AbstractEntityGroupMatchFunctor extends AbstractMatchFunctor {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractEntityGroupMatchFunctor.class);

    /** The entity group to match against. */
    private String entityGroup;

    /**
     * Gets the entity group to match against.
     * 
     * @return entity group to match against
     */
    public String getEntityGroup() {
        return entityGroup;
    }

    /**
     * Sets the entity group to match against.
     * 
     * @param group entity group to match against
     */
    public void setEntityGroup(String group) {
        entityGroup = DatatypeHelper.safeTrimOrNullString(group);
    }

    /**
     * Checks if the given entity is in the provided entity group.
     * 
     * @param entity the entity to check
     * 
     * @return true if the entity is in the group, false if not
     */
    protected boolean isEntityInGroup(EntityDescriptor entity) {
        if (entityGroup == null) {
            log.debug("No entity group specificed, unable to check if entity is in group");
            return false;
        }

        if (entity == null) {
            log.debug("No entity metadata available, unable to check if entity is in group {}", entityGroup);
            return false;
        }

        EntitiesDescriptor currentGroup = (EntitiesDescriptor) entity.getParent();
        if (currentGroup == null) {
            log.debug("No entity descriptor does not have a parent object, unable to check if entity is in group {}",
                    entityGroup);
            return false;
        }

        do {
            if (entityGroup.equals(currentGroup.getName())) {
                return true;
            }
            currentGroup = (EntitiesDescriptor) currentGroup.getParent();
        } while (currentGroup.getParent() != null);

        return false;
    }
}