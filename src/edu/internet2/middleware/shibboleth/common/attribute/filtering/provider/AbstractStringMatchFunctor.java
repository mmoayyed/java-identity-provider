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

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider;


/**
 * Base class for {@link MatchFunctor} that match one string value against a given string value.
 */
public abstract class AbstractStringMatchFunctor extends AbstractMatchFunctor {

    /** String to match for a positive evaluation. */
    private String matchString;
    
    /** Whether the match evaluation is case sensitive. */
    private boolean caseSensitive;
    
    /**
     * Gets the string to match for a positive evaluation.
     * 
     * @return string to match for a positive evaluation
     */
    public String getMatchString(){
        return matchString;
    }
    
    /**
     * Sets the string to match for a positive evaluation.
     * 
     * @param match string to match for a positive evaluation
     */
    public void setMatchString(String match){
        matchString = match;
    }
    
    /**
     * Gets whether the match evaluation is case sensitive.
     * 
     * @return whether the match evaluation is case sensitive
     */
    public boolean isCaseSensitive(){
        return caseSensitive;
    }
    
    /**
     * Sets whether the match evaluation is case sensitive.
     * 
     * @param isCaseSensitive whether the match evaluation is case sensitive
     */
    public void setCaseSensitive(boolean isCaseSensitive){
        caseSensitive = isCaseSensitive;
    }

    /**
     * Matches the given value against the provided match string.  {@link Object#toString()} is used to 
     * produce the string value to evaluate.
     * 
     * @param value the value to evaluate
     * 
     * @return true if the value matches the given match string, false if not
     */
    protected boolean isMatch(Object value){
        if(value == null && matchString == null){
            return true;
        }
        
        if(caseSensitive){
            return value.toString().equals(matchString);
        }else{
            return value.toString().equalsIgnoreCase(matchString);
        }
    }
}