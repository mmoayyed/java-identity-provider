function writeLocalStorageAndSubmit(key, value, version) {
    var success;
    try {
    	localStorage.setItem(key, value);
    	localStorage.setItem("shib_idp_ls_version", version);
        success = "true";
    } catch (e) {
        success = "false";
        document.form1["shib_idp_ls_exception"].value = e;
    }
    document.form1["shib_idp_ls_success"].value = success;
    document.form1.submit();
}