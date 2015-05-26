function writeLocalStorageAndSubmit(key, value) {
    var success;
    try {
        localStorage.setItem(key, value);
        success = "true";
    } catch (e) {
        success = "false";
        document.form1["shib_idp_ls_exception"].value = e;
    }
    document.form1["shib_idp_ls_success"].value = success;
    document.form1.submit();
}