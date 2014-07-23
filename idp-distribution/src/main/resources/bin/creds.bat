@echo off

rem This is a rudimentary script to generate credentials for the IdP.
rem Run from within the idp.home directory as bin\creds.bat.
rem See conf\generate-credentials.xml to customize.

java -cp "bin\lib\*;war\WEB-INF\lib\*" net.shibboleth.idp.GenerateCredentials

rem TODO generate creds/sealer.jks with parameters from conf/idp.properties
keytool -genseckey -alias idpSecretKey -keypass password -storepass password -storetype JCEKS -keyalg AES -keysize 256 -keystore creds/sealer.jks
