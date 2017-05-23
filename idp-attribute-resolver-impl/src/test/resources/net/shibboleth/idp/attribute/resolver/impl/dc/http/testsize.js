importClass(Packages.java.util.HashSet);
importClass(Packages.net.shibboleth.utilities.java.support.httpclient.HttpClientSupport);
importPackage(Packages.net.shibboleth.idp.attribute);

var body = HttpClientSupport.toString(response.getEntity(), "UTF-8", 10);
var result = JSON.parse(body);

for (var i=0; i<result.length; i++) {

    var attr = new IdPAttribute(result[i].name);
    var values = new HashSet();
    
    for (var j=0; j<result[i].values.length; j++) {
        values.add(new StringAttributeValue(result[i].values[j]));
    }
    
    attr.setValues(values);
    connectorResults.add(attr);
}
