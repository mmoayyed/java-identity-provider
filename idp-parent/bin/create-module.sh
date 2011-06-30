#!/bin/bash

MODULE_NAME=$1
if [ -z "$MODULE_NAME" ]; then
    echo "Module name must be supplied";
    exit 1
fi
MODULE_DIR="idp-$MODULE_NAME"

DESCRIPTIVE_NAME=$2
if [ -z "$DESCRIPTIVE_NAME" ]; then
    echo "Descriptive name must be supplied"
    exit 1
fi

if [ ! -e "idp-parent" -o  ! -e  "../java-identity-provider" ]; then
    echo "You must run this script from within the java-identity-provider directory"
    exit 1
fi

#1. Create a directory called idp-MODULE_NAME as a child of the java-identity-provider
#   project
#

[ ! -e $MODULE_DIR ] && svn mkdir $MODULE_DIR

#2. With in the module directory create the directories:
#   - src/main/java
#   - src/main/resources
#   - src/test/java
#   - src/test/resources

for dir in src/main/java src/main/resources src/test/java src/test/resources; do
    newdir=$MODULE_DIR/$dir
    [ ! -e $newdir ] && svn --parents mkdir $newdir
done

#
#3. Copy the java-identity-provider/idp-parent/module-settings/pom-template.xml in to 
#   your module directory and edit the following lines:
#   - line 14, replace "DESCRIPTIVE NAME" with a decently descriptive name
#   - line 15, replace "MODULE_NAME"

[ ! -e $MODULE_DIR/pom.xml ] && svn cp idp-parent/module-settings/pom-template.xml $MODULE_DIR/pom.xml
perl -p -i -e "s/MODULE_NAME/${MODULE_NAME}/g; s/DESCRIPTIVE_NAME/${DESCRIPTIVE_NAME}/g"  $MODULE_DIR/pom.xml

#
#4. Copy the java-identity-provider/idp-parent/module-settings/eclipse/.project-template in to 
#   your module directory and edit the following lines:
#   - line 3, replace "MODULE_NAME"

[ ! -e $MODULE_DIR/.project ] && svn cp idp-parent/module-settings/eclipse/.project-template $MODULE_DIR/.project
perl -p -i -e "s/MODULE_NAME/${MODULE_NAME}/g"  $MODULE_DIR/.project

#
#5. Copy the java-identity-provider/idp-parent/module-settings/eclipse/.classpath-template in to 
#   your module directory.

[ ! -e $MODULE_DIR/.classpath ] && svn cp idp-parent/module-settings/eclipse/.classpath-template $MODULE_DIR/.classpath

#
#6. Add (svn add java-identity-provider/idp-MODULE_NAME) your module to SVN.
#   
#7. From the java-identity-provider directory, run the following SVN command:
#   svn propset svn:externals -F idp-parent/module-settings/externals.svn idp-MODULE_NAME

svn propset svn:externals -F idp-parent/module-settings/externals.svn $MODULE_DIR

#   
#8. From the java-identity-provider directory, run the following SVN command:
#   svn propset svn:ignore -F idp-parent/module-settings/ignore.svn idp-MODULE_NAME

svn propset svn:ignore -F idp-parent/module-settings/ignore.svn $MODULE_DIR

#   
#9. Commit (svn commit idp-MODULE_NAME -m "LOG MESSAGE") your module to SVN

svn commit $MODULE_DIR -m "Create module $MODULE_NAME" 

#10. Perform an svn update to pull in the externalized files (set up via step 5)

svn update $MODULE_DIR

#11. Add module to the idp-parent/pom.xml and commit change to this POM.

echo ""
echo ""
echo "****************************************************"
echo ""
echo "IMPORTANT!"
echo ""
echo "Don't forget to add the module to the idp-parent/pom.xml" and commit it"
echo ""
echo ""
echo "****************************************************"
