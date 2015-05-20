// TODO use setItem
function writeLocalStorageAndSubmit() {
    localStorage["shib_idp_ls"] = document.form1["shib_idp_ls"].value;
    document.form1.submit();
}