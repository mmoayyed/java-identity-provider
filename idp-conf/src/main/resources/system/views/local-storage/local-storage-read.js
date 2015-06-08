function readLocalStorageAndSubmit(key, version) {
    var localStorageSupported = isLocalStorageSupported();
    document.form1["shib_idp_ls_supported"].value = localStorageSupported;
    if (localStorageSupported) {
        var success;
        try {
            var clientVersion = localStorage.getItem("shib_idp_ls_version");
            if (clientVersion != null) {
                document.form1["shib_idp_ls_version"].value = clientVersion;
                if (clientVersion > version) {
                    var value = localStorage.getItem(key);
                    if (value != null) {
                        document.form1["shib_idp_ls_value"].value = value;
                    }
                }
            }
            success = "true";
        } catch (e) {
            success = "false";
            document.form1["shib_idp_ls_exception"].value = e;
        }
        document.form1["shib_idp_ls_success"].value = success;
    }
    document.form1.submit();
}
function isLocalStorageSupported() {
    try {
        localStorage.setItem("shib_idp_ls_test", "shib_idp_ls_test");
        localStorage.removeItem("shib_idp_ls_test");
        return true;
    } catch (e) {
        return false;
    }
}
