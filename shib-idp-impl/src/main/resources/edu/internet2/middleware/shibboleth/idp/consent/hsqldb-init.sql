DROP TABLE AttributeReleaseConsent IF EXISTS;
DROP TABLE AgreedTermsOfUse IF EXISTS;
DROP TABLE RelyingParty IF EXISTS;
DROP TABLE Principal IF EXISTS;

CREATE TABLE Principal (
    uniqueId            VARCHAR(256)    NOT NULL,
    firstAccess         TIMESTAMP       DEFAULT 'NOW' NOT NULL,
    lastAccess          TIMESTAMP       DEFAULT 'NOW' NOT NULL,
    globalConsent       BIT             DEFAULT FALSE NOT NULL,
 
    PRIMARY KEY(uniqueId)
);

CREATE TABLE RelyingParty (
    entityId            VARCHAR(256)                  NOT NULL,
    
    PRIMARY KEY(entityId)
);

CREATE TABLE AgreedTermsOfUse (
    principalId         VARCHAR(256)                  NOT NULL,
    version             VARCHAR(256)    DEFAULT '0.0' NOT NULL,
    fingerprint         VARCHAR(256)    DEFAULT ''    NOT NULL,
    agreeDate           TIMESTAMP       DEFAULT 'NOW' NOT NULL,
    
    PRIMARY KEY (principalId, version),
    
    FOREIGN KEY (principalId) REFERENCES Principal(uniqueId) ON DELETE CASCADE
);

CREATE TABLE AttributeReleaseConsent (
    principalId         VARCHAR(256)                  NOT NULL,
    relyingPartyId      VARCHAR(256)                  NOT NULL,
    attributeId         VARCHAR(256)                  NOT NULL,
    attributeValuesHash VARCHAR(256)    DEFAULT ''    NOT NULL,
    releaseDate         TIMESTAMP       DEFAULT 'NOW' NOT NULL,

    PRIMARY KEY (principalId, relyingPartyId, attributeId),
    
    FOREIGN KEY (principalId)    REFERENCES Principal(uniqueId)    ON DELETE CASCADE,
    FOREIGN KEY (relyingPartyId) REFERENCES RelyingParty(entityId) ON DELETE CASCADE
);