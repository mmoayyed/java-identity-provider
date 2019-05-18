importPackage(Packages.net.shibboleth.idp.attribute);
importPackage(Packages.java.util);
importPackage(Packages.java.lang);

attr = new IdPAttribute("ScriptedOne");
set = new LinkedHashSet(2);
set.add(new StringAttributeValue("Value 1"));
set.add(new StringAttributeValue("Value 2"));
attr.setValues(set);
connectorResults.add(attr);

attr = new IdPAttribute("TwoScripted");
set = new LinkedHashSet(4);
set.add(new StringAttributeValue("1Value"));
set.add(new StringAttributeValue("2Value"));
set.add(new StringAttributeValue("3Value"));
attr.setValues(set);
connectorResults.add(attr);

attr = new IdPAttribute("Subjects");
set = new LinkedHashSet(4);
x = subjects[0].getPrincipals().iterator();
while(x.hasNext()){
    set.add(new StringAttributeValue(x.next().getName()));
    }
x = subjects[1].getPrincipals().iterator();
while (x.hasNext()){
    set.add(new StringAttributeValue(x.next().getName()));
    }
attr.setValues(set);
connectorResults.add(attr);



child = profileContext.getSubcontext("net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext");
attr = new IdPAttribute("ThreeScripted");
set = new HashSet(1);
set.add(new StringAttributeValue(child.getClass().getSimpleName()));
attr.setValues(set);
connectorResults.add(attr);
