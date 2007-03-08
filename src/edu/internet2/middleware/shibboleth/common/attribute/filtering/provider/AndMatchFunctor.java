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

import java.util.List;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.FilterContext;

/**
 * A {@link MatchFunctor} that logical ANDs the results of contained functors.
 */
public class AndMatchFunctor extends AbstractMatchFunctor {
    
    /** Contained functors. */
    private List<MatchFunctor> childFunctors;
    
    /**
     * Gets the functors whose results will be ANDed.
     * 
     * @return functors whose results will be ANDed
     */
    public List<MatchFunctor> getFunctors(){
        return childFunctors;
    }
    
    /**
     * Sets the functors whose results will be ANDed.
     * 
     * @param functors functors whose results will be ANDed
     */
    public void setFunctors(List<MatchFunctor> functors){
        childFunctors = functors; 
    }

    /** {@inheritDoc} */
    protected boolean doEvaluate(FilterContext filterContext) throws FilterProcessingException {
        
        if(childFunctors == null){
            return false;
        }
        
        for(MatchFunctor child : childFunctors){
            if(!child.evaluate(filterContext)){
                return false;
            }
        }
        
        return true;
    }

    /** {@inheritDoc} */
    protected boolean doEvaluate(FilterContext filterContext, String attributeId, Object attributeValue)
            throws FilterProcessingException {
        if(childFunctors == null){
            return false;
        }
        
        for(MatchFunctor child : childFunctors){
            if(!child.evaluate(filterContext, attributeId, attributeValue)){
                return false;
            }
        }
        
        return true;
    }
}