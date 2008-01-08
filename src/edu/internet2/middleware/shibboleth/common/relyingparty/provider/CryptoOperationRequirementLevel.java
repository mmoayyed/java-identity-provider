package edu.internet2.middleware.shibboleth.common.relyingparty.provider;

/** Indicates the requirement level for crypto operations like signing and encryption. */
public enum CryptoOperationRequirementLevel{
    /** Indicates that the operation must always be performed. */
    always, 
    
    /** Indicates that the operation should only be performed if the binding/transport does not provide equivalent protection. */
    conditional, 
    
    /** Indicates the operation should never be performed.*/
    never
}