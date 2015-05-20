function readLocalStorageAndSubmit() {
    var localStorageSupported = isLocalStorageSupported();
    document.form1["shib_idp_ls_support"].value = localStorageSupported;
    if (localStorageSupported) {
        document.form1["shib_idp_ls"].value = localStorage.getItem("shib_idp_ls");
    }
    document.form1.submit();
}
function isLocalStorageSupported() {
    try {
        localStorage.setItem("testKey", "testValue");
        localStorage.removeItem("testKey");
        return true;
    } catch (e) {
        return false;
    }
}
