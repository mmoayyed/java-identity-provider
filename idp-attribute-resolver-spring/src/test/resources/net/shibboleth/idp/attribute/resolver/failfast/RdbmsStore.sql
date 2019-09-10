CREATE TABLE people (
        userid VARCHAR(50) NOT NULL,
        name VARCHAR(50) NOT NULL,
        password VARCHAR(50) NOT NULL,
        homephone VARCHAR(15) NOT NULL,
        mail VARCHAR(100),
        description VARCHAR(250)
        );

CREATE TABLE groups (
        userid VARCHAR(50) NOT NULL,
        name VARCHAR(50) NOT NULL
        );
        
CREATE TABLE shibpid (
    localEntity VARCHAR(255) NOT NULL, 
    peerEntity VARCHAR(255) NOT NULL,
    persistentId VARCHAR(50) NOT NULL, 
    principalName VARCHAR(50) NOT NULL, 
    localId VARCHAR(50) NOT NULL, 
    peerProvidedId VARCHAR(50) NULL, 
    creationDate TIMESTAMP NOT NULL, 
    deactivationDate TIMESTAMP NULL,
    PRIMARY KEY (localEntity, peerEntity, persistentId)
    );
