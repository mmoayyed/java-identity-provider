function readLocalStorageAndSubmit(key) {
    var localStorageSupported = isLocalStorageSupported();
    document.form1["shib_idp_ls_supported"].value = localStorageSupported;
    if (localStorageSupported) {
        var success;
        try {
            document.form1["shib_idp_ls_value"].value = localStorage.getItem(key);
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
