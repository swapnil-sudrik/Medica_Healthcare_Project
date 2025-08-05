$(document).ready(function(){
  $("#tm_id").select2();
  $("#im_id").select2();
  $("#hostCode").select2();
  // Read selected option
  $("#im_id").change(function(){
       /*  alert('Selected value: ' + $(this).val()); */
        $("#tm_id").empty();
        var test = $("#im_id").val();
        console.log(test);
         $.ajax({
                type: "GET",
                url: "/testmaster_list",
                data: "im_id="+test,
                dataType: "html",
                success: function (data)
                {

                    var details =  JSON.parse(data);
                    console.log(details)
                    var cnt = details.length;
                    console.log(cnt)
                    for(var i=0;i<cnt;i++){
                    //alert(city_detail[i].cityName);

                    var a ='<option value="'+details[i].tm_id+'">'+details[i].parameter_name+'</option>';
                    $("#tm_id").append(a);
                    }

                }
            });

    });

    /* On Edit */
    if(document.getElementById('isEdit').innerHTML == 'Edit Range') {
        let toAgeTypes = document.getElementById("toAgeType").options.length;
        let fromAgeTypes = document.getElementById("fromAgeType").options.length
        let ToAgeType = document.getElementById("toAgeType").value;
        let FromAgeType = document.getElementById("fromAgeType").selectedIndex;
        for (var i = 0; i < document.getElementById("toAgeType").options.length; i++) {
                    if (document.getElementById("toAgeType").options[i].value == document.getElementById("toAgeType")[ToAgeType].value) {
                        document.getElementById("toAgeType").options[ToAgeType].value = true;
                   }
              }
        for(let i=0; i <= fromAgeTypes; i++) {
                if (document.getElementById("fromAgeType").options[i].value == document.getElementById("fromAgeType")[FromAgeType].value) {
                  document.getElementById("fromAgeType").options[ToAgeType] = true;
                 }
            }
        }

});
let addRow = function () {
    let listName = 'RRange'; //list name in RangeMaster.class
    let fieldsNames = ['fromAge', 'toAge', 'male','female']; //field names from ReferanceRange.class
    let fieldstype = ['number', 'number', 'text','text'];
    let rowIndex = document.querySelectorAll('.item').length; //we can add mock class to each reference-row

    let row = document.createElement('div');
    row.classList.add('row', 'item');

    fieldsNames.forEach((fieldName) => {

        let col = document.createElement('div');
        col.classList.add('col', 'form-group');
        if (fieldName === 'id') {
            col.classList.add('d-none'); //field with id - hidden
        }
        let i = fieldsNames.indexOf(fieldName);


        let input = document.createElement('input');
        input.type = fieldstype[i];
        input.classList.add('form-control');
        input.id = listName + rowIndex + '.' + fieldName;
        input.required = "required";
        input.setAttribute('name',  fieldName);

        col.appendChild(input);
        row.appendChild(col);
    });

    document.getElementById('refrenceList').appendChild(row);
};

let removeRow = function () {

    var header = document.getElementById('refrenceList');
    var btns = header.getElementsByClassName("item");
    var h = (btns.length) - 1;
    btns[h].remove();


};

var app=angular.module('ngRange',[]).config(function ($httpProvider) {
    $httpProvider.defaults.headers.common['Cache-Control'] = 'no-cache';
    $httpProvider.defaults.cache = false;
  });

app.controller('rangeServicesController', function rangeServicesController($scope,$http) {
    /* fromAge  */
        $scope.calAge_fromAge = function() {
            console.log("age is", document.getElementById('getAge_fromAge').value, $scope.selectedvalue_fromAge);
            let givenAge_fromAge = document.getElementById('getAge_fromAge').value;
            console.log("fromAge", document.getElementById("fromAge").innerHTML);
            let ageInDays_fromAge;

            if($scope.selectedvalue_fromAge == 'months') {

                /* months to days calculation */

                ageInDays_fromAge = (givenAge_fromAge * 365)/12;
                console.log("ageInDays_fromAge and value in month", ageInDays_fromAge);
                console.log("fromAge", document.getElementById("fromAge").value = Math.round(ageInDays_fromAge));

            } else if($scope.selectedvalue_fromAge == 'years') {

                /* years to days calculation */

                let yearsDays = (givenAge_fromAge * 365);
                let leapValue = (givenAge_fromAge/4);
                ageInDays_fromAge = yearsDays + leapValue;
                console.log("ageInDays_fromAge and value get in years", ageInDays_fromAge);
                console.log("fromAge", document.getElementById("fromAge").value = Math.round(ageInDays_fromAge));

            } else if($scope.selectedvalue_fromAge == 'weeks') {
                /* weeks in days conculation */
                ageInDays_fromAge = (givenAge_fromAge * 7);
                console.log("ageInDays and value get in weeks", ageInDays_fromAge);
                console.log("fromAge", document.getElementById("fromAge").value = Math.round(ageInDays_fromAge));
            } else {
                ageInDays_fromAge = (givenAge_fromAge);
                console.log("ageInDays and value get in weeks", ageInDays_fromAge);
                console.log("fromAge", document.getElementById("fromAge").value = Math.round(ageInDays_fromAge));
            }
        }

        /* toAge */
            $scope.calAge_toAge = function() {
            console.log("age is of toAge", document.getElementById('getAge_toAge').value, $scope.selectedvalue_toAge);
            let givenAge_toAge = document.getElementById('getAge_toAge').value;
            console.log("toAge", document.getElementById("toAge").innerHTML);
            let ageInDays_toAge;

            if($scope.selectedvalue_toAge == 'months') {

                /* months to days calculation */

                ageInDays_toAge = (givenAge_toAge * 365)/12;
                console.log("ageInDays_toAge and value in month", ageInDays_toAge);
                console.log("toAge", document.getElementById("toAge").value = Math.round(ageInDays_toAge));

            } else if($scope.selectedvalue_toAge == 'years') {

                /* years to days calculation */
                let yearsDays = (givenAge_toAge * 365);
                let leapValue = (givenAge_toAge/4);
                ageInDays_toAge = yearsDays + leapValue;
                console.log("ageInDays and value get in years", ageInDays_toAge);
                console.log("toAge", document.getElementById("toAge").value = Math.round(ageInDays_toAge));

            } else if($scope.selectedvalue_toAge == 'weeks') {
                /* weeks in days conculation */

                ageInDays_toAge = (givenAge_toAge * 7);
                console.log("ageInDays_toAge and value get in weeks", ageInDays_toAge);
                console.log("toAge", document.getElementById("toAge").value = Math.round(ageInDays_toAge));
            } else {
                /* days */
                ageInDays_toAge = (givenAge_toAge);
                console.log("ageInDays_toAge and value get in days", ageInDays_toAge);
                console.log("toAge", document.getElementById("toAge").value = Math.round(ageInDays_toAge));
            }
        }
    });
