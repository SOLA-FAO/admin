$(function () {
    $("[rel='tooltip']").tooltip();
});

function limitSpecialKeys(event) {
    var regex = new RegExp("^[a-zA-Z0-9_]+$");
    var key = String.fromCharCode(!event.charCode ? event.which : event.charCode);
    if (event.keyCode !== 8 && event.keyCode !== 27 && event.keyCode !== 46 && 
            !(event.keyCode >= 35 && event.keyCode <= 40) && !regex.test(key)) {
       event.preventDefault();
       return false;
    }
}