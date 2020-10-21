var result = JSON.parse(record.getValue());

var IdPAttribute = Java.type("net.shibboleth.idp.attribute.IdPAttribute");
var StringValue = Java.type("net.shibboleth.idp.attribute.StringAttributeValue");
var HashSet = Java.type("java.util.HashSet");

for (var i=0; i<result.length; i++) {

    var attr = new IdPAttribute(result[i].name);
    var values = new HashSet();
    
    for (var j=0; j<result[i].values.length; j++) {
        values.add(new StringValue(result[i].values[j]));
    }
    
    attr.setValues(values);
    connectorResults.add(attr);
}
