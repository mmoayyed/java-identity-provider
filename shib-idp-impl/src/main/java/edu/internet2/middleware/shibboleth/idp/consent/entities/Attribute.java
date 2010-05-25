/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.consent.entities;

import java.util.Collection;
import edu.vt.middleware.crypt.digest.SHA256;
import edu.vt.middleware.crypt.util.HexConverter;

/**
 *
 */
public class Attribute {

    private final String id;

    private final Collection<String> values;

    private final String valuesHash;

    private String displayName;
    
    private String displayDescription;

    
    public Attribute(final String id, final String valuesHash) {
    	this.id = id;
    	this.valuesHash = valuesHash;
    	values = null;
    }
    
    public Attribute(final String id, final Collection<String> values) {
    	this.id = id;
    	this.values = values;  	
    	this.valuesHash = new SHA256().digest(values.toString().getBytes(), new HexConverter());
    }
    
    /**
     * @return Returns the id.
     */
    public final String getId() {
        return id;
    }

    /**
     * @return Returns the values.
     */
    public final Collection<String> getValues() {
        return values;
    }
    
    /**
     * @return Returns the valuesHash.
     */
    public final String getValuesHash() {
        return valuesHash;
    }
    
    /**
	 * @return Returns the displayName.
	 */
	public final String getDisplayName() {
		return displayName;
	}

	/**
	 * @param displayName The displayName to set.
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return Returns the displayDescription.
	 */
	public final String getDisplayDescription() {
		return displayDescription;
	}

	/**
	 * @param displayDescription The displayDescription to set.
	 */
	public void setDisplayDescription(String displayDescription) {
		this.displayDescription = displayDescription;
	}

	/** {@inheritDoc} */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/** {@inheritDoc} */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Attribute other = (Attribute) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (valuesHash == null) {
			if (other.valuesHash != null)
				return false;
		} else if (!valuesHash.equals(other.valuesHash))
			return false;
		return true;
	}

	/** {@inheritDoc} */
    @Override
    public String toString() {
        return id;
    }

}
