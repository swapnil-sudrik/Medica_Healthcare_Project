var app = angular.module('nglis', []).config(function ($httpProvider) {
  $httpProvider.defaults.headers.common['Cache-Control'] = 'no-cache';
  $httpProvider.defaults.cache = false;
});
$("select").select2({
  width: '187px',
  display: 'none'
});
app.controller('searchServicesController', function SearchDoctorController(
  $scope, $http) {
  $scope.allOdArr = [];
  $scope.selectOdId = [];
  $scope.changeValueOd = 0;
  $scope.consoleRes = [];
  $scope.selectedLisId;
  $scope.isFirst = [];
  $scope.selectedResVal;
  $("#showingResult").hide();
  $scope.allDetails = function () {
    $("#showingResult").hide();
    $('.chkAllbox:checkbox').prop('checked', false);
    $scope.selectOdId = [];
    let date = $('.date').val();
    $("#LoadingImage").show();
    let barcode = $("#barcode").val(); 
    let machineStatus = $("#machineStatus").val() || "2";
    let referenceRange = $("#referenceRange").val() || "0";
    let formattedDate = new Date(date).toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });;
    $http.get(
      "/sampleTransferConsoleSearch?date=" + date +
      "&barcodeId=" + barcode +
      "&machineStatus=" + machineStatus +
      "&referenceRange=" + referenceRange
    ).then(
      function (response) {
        $scope.consoleRes = response.data;
        $("#LoadingImage").hide();
        $("#barcode").val('');
        if (barcode) {
          $("#showingResult").text("Showing result of the barcode: " + barcode).show();
        } else {
          $("#showingResult").text("Showing result of the date: " + formattedDate ).show();
        }
      });
  }
  //on search click
  $('.getResult').on(
    'click',
    function () {
      $scope.allDetails();
    });

  $scope.postaUpload = function () {
    if ($scope.selectOdId.length > 0) {
      var uniqueIds = new Set($scope.selectOdId);
      var totalOdrr = Array.from(uniqueIds);
      $http.post("/ajax/transferTestsSuperHubToHub?oIds=" + totalOdrr+"&dbContext="+$('#dbcontext').val())
        .then((response) => {
          swal("Test Transferred Successfully");
          $(".getResult").click();
        }, (error) => {
          swal("Oops!", "Something went wrong");
        });
    }
  }
  $('.uploadData').on(
    'click',
    function () {
      $scope.postaUpload();
    });

  //check all button
  $('#chkAllbox').on('change', function () {
    $scope.selectOdId = [];
    if ($(this).is(':checked')) {
      $('.chkODSingleBox:checkbox').prop('checked', true);
      angular.forEach($scope.consoleRes, function (item) {
        $scope.allOdArr.push(item.orderId);
      });
      var uniqueIds = new Set($scope.allOdArr);
      $scope.selectOdId = Array.from(uniqueIds);
    } else {
      $('.chkODSingleBox:checkbox').prop('checked', false);
      $scope.selectOdId = [];
      $scope.allOdArr = [];
    }
    updateTransferButtonState();
  });


  $scope.isFirstInstance = function (index) {
    var currentOrderId = $scope.consoleRes[index].orderId;
    var previousOrderId = index > 0 ? $scope.consoleRes[index - 1].orderId : null;
    $scope.isFirst[index] = currentOrderId !== previousOrderId;
  };

  $scope.toggleSelectionCleck = function (orderId) {
    var indexIvid = $scope.selectOdId.indexOf(orderId);
    if ($('#' + orderId).is(':checked')) {
      if (indexIvid === -1) {
        $scope.selectOdId.push(orderId);
      }
    } else {
      $('.chkAllbox:checkbox').prop('checked', false);
      if (indexIvid !== -1) {
        $scope.selectOdId.splice(indexIvid, 1);
      }
    }
    updateTransferButtonState();
  };

  function updateTransferButtonState() {
    var transferButton = $('.uploadData');
    if ($scope.selectOdId.length > 0) {
      transferButton.prop('disabled', false);
    } else {
      transferButton.prop('disabled', true);
    }
  }

  ///updating result function starts
  $scope.setSelectedValue = function (lrId) {
    $scope.selectedResVal = $('#' + lrId).val();
    $scope.selectedLisId = lrId;
}

$scope.updateResultValue = function () {
    let id = $scope.selectedLisId;
    let editedResult = $('#updatedRes').val();
    $http.post('/ajax/parameterResultUpdate?id=' + id + "&editValue=" + editedResult+"&dbContext="+$('#dbcontext').val())
        .then(function (response) {

            $scope.consoleRes.filter(function (res) {
                if (res.lrId == id) {
                    res.result = editedResult;
                    res.status = 'RESULT_SAVED';
                    $scope.selectedResVal = null; 
                }
            });
        });
}
  ///updating result function end

});
$(document).ready(function () {
  var now = new Date();
  var month = (now.getMonth() + 1);
  var day = now.getDate();
  if (month < 10)
    month = "0" + month;
  if (day.toLocaleString().length <= 1) {
    day = "0" + day;
  }
  var today = now.getFullYear() + '-' + month + '-' + day;
  $('#date').val(today);
  // $(".getResult").click();
  //Search by clicking enter
  $("#barcode").keypress(function (event) {
    if (event.keyCode === 13) {
      $(".getResult").click();
    }
  });
});