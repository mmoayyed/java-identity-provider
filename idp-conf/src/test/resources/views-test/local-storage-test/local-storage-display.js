function localStorageGetItem(key) {
    document.getElementById("localStorageGetItem").innerHTML = "<textarea>" + localStorage.getItem(key) + "</textarea>";
}