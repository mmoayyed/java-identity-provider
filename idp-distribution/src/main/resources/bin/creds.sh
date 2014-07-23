#! /bin/sh

# This is a rudimentary script to generate credentials for the IdP.
# Run from within the idp.home directory as bin/creds.sh.
# See conf/generate-credentials.xml to customize.

java -cp "bin/lib/*:war/WEB-INF/lib/*" net.shibboleth.idp.GenerateCredentials

# TODO generate creds/sealer.jks with parameters from conf/idp.properties
keytool -genseckey -alias idpSecretKey -keypass password -storepass password -storetype JCEKS -keyalg AES -keysize 256 -keystore creds/sealer.jks
