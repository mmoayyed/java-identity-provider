CREATE TABLE User (
    id         			VARCHAR(256)                  				NOT NULL,
    globalConsent       BOOLEAN			DEFAULT FALSE				NOT NULL, 
    PRIMARY KEY(id)
);

CREATE TABLE AttributeRelease (
    userId         		VARCHAR(256)                  				NOT NULL,
    relyingPartyId      VARCHAR(256)                  				NOT NULL,
    attributeId         VARCHAR(256)                  				NOT NULL,
    valuesHash 			VARCHAR(256)								NOT NULL,
    consentDate        	TIMESTAMP       DEFAULT CURRENT_TIMESTAMP	NOT NULL,

    PRIMARY KEY (userId, relyingPartyId, attributeId),   
    FOREIGN KEY (userId)	REFERENCES User(id)						ON DELETE CASCADE
);