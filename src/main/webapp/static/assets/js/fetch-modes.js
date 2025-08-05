
var app = angular.module('ngndhm', []).config(function($httpProvider) {
	$httpProvider.defaults.headers.common['Cache-Control'] = 'no-cache';
	$httpProvider.defaults.cache = false;
});


app.controller('searchServicesController', function SearchDoctorController(
	$scope, $http) {

	//fetch all details
	$scope.confirmMode = function() {

		$scope.consoleRes = [];
		let hipid = $('#hipid').val();
		let purpose = $('#purpose').val();
		let hipType = $('#hipType').val();
		let healthId = $('#healthId').val();
		$('#confirm_button').text('verifying....');
		$('#confirm_button').prop('Disabled', true);


		$http.get(
			"/app/fetch-data?healthId=" + healthId
			+ "&hipid=" + hipid+ "&purpose=" + purpose+ "&hipType=" + hipType).then(
				function(response) {
				   swal({
                       type: 'success',
                                  title: 'Success!',
                                  text: 'Verified Successfully!',
                                  buttonsStyling: false,
                                  confirmButtonClass: 'btn btn-lg btn-success'
                   });

					$scope.consoleRes = response.data;
					$('#confirm_button').hide();
                    $('#confirm_button').prop('disabled', false);
					$('#confirm_button').text('Confirm');
					$('#hipid').prop('readonly', true);
					$('#healthId').prop('readonly', true);
					$('#hipType').prop('readonly', true);
					$('#purpose').prop('readonly', true);
					$('#detail_button').show();
					$('#authmodesdisplay').show();
					$("#LoadingImage").hide();


				});
	}

	//on confirm_button click
	$('#confirm_button').on(
		'click',
		function() {

			$scope.confirmMode();
		});


});





