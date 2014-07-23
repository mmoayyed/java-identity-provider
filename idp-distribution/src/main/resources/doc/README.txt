Shibboleth is a federated web authentication and attribute exchange system
based on SAML, originally developed by Internet2 and now a product of the
Shibboleth Consortium.

Please review the terms described in the LICENSE.txt file before using this
code. It is the standard Apache 2.0 license.

A wealth of information about Shibboleth can be found at
http://shibboleth.net/

Shibboleth is divided into identity and service provider components, with the
IdP in Java and the SP (this software) in C++.

Source and binary distributions are available from
http://shibboleth.net/downloads/

The source is available in Subversion, as described in the Shibboleth
site. Mailing lists and a bug database (https://issues.shibboleth.net/) are
also available.

For basic information on building from source, using binaries, and deploying
Shibboleth, refer to the web site and Wiki for the latest documentation.

Alpha 1 Notes 
====================

This is the first alpha release of the 3.0 Identity Provider software.
Refer to https://wiki.shibboleth.net/confluence/display/IDP30/ConfigurationGuide
for information on configuration and testing.

This is alpha software; as such, we do not recommend deploying this code in
production. It is a fairly advanced alpha and represents preliminary decisions
on what the final software will look like, but feedback on configuration and
defaults is very valuable at this stage.

NOTE especially that there is not yet a true installation process, and the
private and secret keys that are included by default are not generated, but
are dummy keys that are checked into subversion and are part of the testing
environment. Using the software for any sensitive purpose is ill advised,
but would require at minimum replacing those keys.

Most, though not all, features supported by 2.4.0 are included in this release.