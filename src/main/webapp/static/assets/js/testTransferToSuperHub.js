/*Author : Deepshikha Rajput, Date: 25 May 2022*/
var app = angular.module('nglis', []).config(function($httpProvider) {
	$httpProvider.defaults.headers.common['Cache-Control'] = 'no-cache';
	$httpProvider.defaults.cache = false;
});
$("select").select2({
	width: '187px',
	display: 'none'
});
app.controller('searchServicesController', function SearchDoctorController(
	$scope, $http) {
  $scope.selectOdId = [];
  $scope.currentPage = 1;
  $scope.totalPages = 0;

  // fetch all details
  $scope.allDetails = function () {
    $scope.selectOdId = [];
    let s_date = $('#starts_at').val();
    let e_date = $('#ends_at').val();
    $("#LoadingImage").show();

    $http.get(
      "/testTransferToSuperHubSearch?starts_at=" + s_date
      + "&ends_at=" + e_date
      + "&test=" + $("#test").val()
      + "&lab=" + $("#lab").val()
      + "&machine=" + $("#machine").val()
      + "&barcode=" + $("#barcode").val()
    ).then(
      function (response) {
        $scope.consoleRes1 = response.data;
        $scope.consoleRes = response.data.content;
        $scope.totalPages = $scope.consoleRes1.totalPages;
        $(".impline").show();
        $("#LoadingImage").hide();

        // Page count
        var range = [];
        for (var i = 0; i < $scope.consoleRes1.totalPages; i++) {
          range.push(i);
        }
        $scope.range = range;
        $scope.currentPage = 0;
        $(".footerPage").show();
      }
    );
  }

  // on search click
  $('.getResult').on('click', function() {
    $scope.allDetails();
  });

  // on page number click
  $scope.nextpagelist = function (n) {
    let s_date = $('#starts_at').val();
    let e_date = $('#ends_at').val();
    $("#LoadingImage").show();
    $(".impline").hide();

    $http.get(
      "/testTransferToSuperHubSearch?starts_at=" + s_date
      + "&ends_at=" + e_date
      + "&test=" + $("#test").val() + "&lab=" + $("#lab").val() + "&page=" + n
    ).then(
      function (response) {
        $scope.consoleRes1 = response.data;
        $scope.consoleRes = response.data.content;
        $("#LoadingImage").hide();

        // Page count
        var range = [];
        for (var i = 0; i < $scope.consoleRes1.totalPages; i++) {
          range.push(i);
        }
        $scope.range = range;
        $(".footerPage").show();
        $scope.currentPage = $scope.consoleRes1.number;
      }
    );
  }
	angular.element(document).ready(function() {
		$scope.optionofSuperhub = '';
		$http.get("/superhubList").then(
			function(response) {
				var j = response.data;
				for (var i = 0; i < j.length; i++) {
					$scope.optionofSuperhub += '<option value="' + j[i].superhubIp + '">' + j[i].superhubName + '</option>';
				}
			});
	});
	//on search click
	$('.getResult').on(
		'click',
		function() {
			$scope.allDetails();
		});


        // Function to calculate age from birthdate
            function calculateAge(birthday) {
                var birthdate = new Date(birthday);
                var today = new Date();
                var age = today.getFullYear() - birthdate.getFullYear();
                var m = today.getMonth() - birthdate.getMonth();
                if (m < 0 || (m === 0 && today.getDate() < birthdate.getDate())) {
                    age--;
                }
                return age;
            }

            $scope.formatDOB = function (dob, gender) {
                var age = calculateAge(dob);
                return age + ' years/' + gender;
            };


	$scope.postaUpload = function(superhub) {
		if ($scope.selectOdId.length > 0) {
			$http.get(
				"/testDetailTransfer?detail=" + $scope.selectOdId+"&superhubIp="+superhub).then((response)=> {
						swal("Awesome!", "Test Sent Successfully");
						$("#upload_od").prop("disabled",false);
						$("#upload_od").text("Transfer");
						$(".getResult").click();
						},(error) =>{

							$("#upload_od").prop("disabled",false);
							$("#upload_od").text("Transfer");
							swal("Opps!", "Something went wrong");
						});
		}
	}
		$scope.showSuperHubListName = function() {
		swal({
			title: "Select SuperHub",
			html: '<select class="swal2-input" id="superhub" > ' +
				$scope.optionofSuperhub +
				' </select>',
			showCancelButton: true,
			closeOnConfirm: false,
		}).then(function() {
			var superhub = $("#superhub").val();
			if (superhub === '') {
				$("#upload_od").prop("disabled",false);
				$("#upload_od").text("Transfer");
				swal("Opps!", "Something went wrong");
			} else {
				$scope.postaUpload(superhub);

			}
		});
	}
	//check all button
	$('#chkAllbox').on(
		'change',
		function() {
			$scope.selectOdId = [];
			if ($("#chkAllbox").is(':checked')) {
				$('.chkODSingleBox:checkbox').prop('checked', true);
				angular.forEach($scope.consoleRes, function(item) {
					$scope.selectOdId.push(item[10]);
				});

			} else {
				$('.chkODSingleBox:checkbox').prop('checked', false);
				$scope.selectOdId = [];
			}
		});
	$scope.toggleSelectionCleck = function(iv_id) {
		var indexIvid = $scope.selectOdId.lastIndexOf(iv_id);
		if ($('#' + iv_id).is(':checked')) {
			$scope.selectOdId.push(iv_id);
		} else {
			$scope.selectOdId.splice(indexIvid, 1);
		}
	}
	 // pagination logic
      $scope.paginate = function (page) {

        $scope.nextpagelist(page);
      };

      $scope.nextPage = function () {
        if ($scope.currentPage < $scope.totalPages - 1) {
          $scope.paginate($scope.currentPage + 1);
        }
      };

      $scope.prevPage = function () {
        if ($scope.currentPage > 0) {
          $scope.paginate($scope.currentPage - 1);
        }
      };
});



//getting end date 24 hours on start date
function startDateChange() {
	let startAt = new Date($('#starts_at').val());
	let e_month, e_date, e_hours, e_min;
	let s_month = startAt.getMonth() + 1;
	if (s_month.toLocaleString().length <= 1) {
		e_month = '0' + (startAt.getMonth() + 1);
	} else {
		e_month = (startAt.getMonth() + 1);
	}
	if (startAt.getDate().toLocaleString().length <= 1) {
		e_date = '0' + (startAt.getDate());
	} else {
		e_date = (startAt.getDate());
	}
	let dateis = startAt.getFullYear() + '-' + e_month + '-'
		+ e_date;
	document.getElementById("ends_at").value = dateis;
}

$(document).ready(function() {
	var now = new Date();
	var month = (now.getMonth() + 1);
	var day = now.getDate();
	let tom_date;
	if (month < 10)
		month = "0" + month;
	if (day.toLocaleString().length <= 1) {
		day = "0" + day;
	}

	var today = now.getFullYear() + '-' + month + '-' + day;
	$('#starts_at').val(today);
	startDateChange();
	$(".getResult").click();

	//Search by clicking enter
           $("#barcode").keypress(function(event){
                if (event.keyCode === 13) {
                    $(".getResult").click();
                }
            });
});




