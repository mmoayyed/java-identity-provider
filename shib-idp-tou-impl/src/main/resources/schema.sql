CREATE TABLE AcceptedToU (
    userId         		VARCHAR(256)                  				NOT NULL,
    version             VARCHAR(256)    DEFAULT '0.0' 				NOT NULL,
    fingerprint         VARCHAR(256)    DEFAULT ''    				NOT NULL,
    acceptanceDate      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP	NOT NULL,
    
    PRIMARY KEY (userId, version)
);