#! /bin/sh

# This is a rudimentary script to generate credentials which should be run from the idp.home directory as bin/creds.sh.

# TODO get parameters from conf/idp.properties

HOSTNAME=idp.example.org

URI_ALT_NAME=https://idp.example.org/idp/shibboleth

CLASS=net.shibboleth.utilities.java.support.security.SelfSignedCertificateGenerator

java -cp "war/WEB-INF/lib/*" $CLASS --hostname $HOSTNAME --keyfile creds/idp-signing.key --certfile creds/idp-signing.crt --uriAltName $URI_ALT_NAME

java -cp "war/WEB-INF/lib/*" $CLASS --hostname $HOSTNAME --keyfile creds/idp-encryption.key --certfile creds/idp-encryption.crt --uriAltName $URI_ALT_NAME

java -cp "war/WEB-INF/lib/*" $CLASS --hostname $HOSTNAME --storefile creds/idp-tls.p12 --storepass changeit --uriAltName $URI_ALT_NAME

# TODO generate creds/sealer.jks
svn export https://svn.shibboleth.net/java-identity-provider/trunk/idp-conf/src/test/resources/creds/sealer.jks creds/sealer.jks
