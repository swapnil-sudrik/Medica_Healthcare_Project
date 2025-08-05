var app = angular.module('ngcms', []).config(function($httpProvider) {
    $httpProvider.defaults.headers.common['Cache-Control'] = 'no-cache';
    $httpProvider.defaults.cache = false;
});

$(document).ready(function() {
    $("#start_date").pickadate({
        max: new Date(),
        clear: ''
    });
    $("#end_date").pickadate({
        max: new Date(),
        clear: ''
    });

    var d = new Date();
    if ($('#start_date').val() == '') {
        $('#start_date').pickadate('picker').set('select', d).trigger("change");
    }
    if ($('#end_date').val() == '') {
        $('#end_date').pickadate('picker').set('select', d).trigger("change");
    }
});

app.controller('searchServicesController', function($scope, $http, $window) {
    $scope.consoleRes = [];
    $scope.qcMapMeanVal = "";
    $scope.coefOfvarient = "";

    $scope.allDetails = function() {
        let start = $('#start_date').val();
        let end = $('#end_date').val();
        let p_name = $('#parameterName').val();
        let lotNo = $('#lotNo').val();
        let level = $('#level').val();


        var startDate = new Date(start.replace(/-/g, "/"));
        var endDate = new Date(end.replace(/-/g, "/"));
        var thirtyOneDaysLater = new Date(startDate);
        thirtyOneDaysLater.setDate(thirtyOneDaysLater.getDate() + 30);

        if (endDate > thirtyOneDaysLater) {
            $scope.alertMessage = "Please select a date range within one month.";
            $("#search").siblings('span.clientmessage').addClass('error').text($scope.alertMessage);
            return false;
        } else if (start == '') {
            $(".start_date").siblings('span.clientmessage').addClass('error').text("Please select the start date.");
            $(".start_date").css('border-color', '#F5A9A9').focus();
            return false;
        } else if (end == '') {
            $(".end_date").siblings('span.clientmessage').addClass('error').text("Please select the end date.");
            $(".end_date").css('border-color', '#F5A9A9').focus();
            return false;
        } else if (startDate > endDate) {
            $("#search").siblings('span.clientmessage').addClass('error').text("Please select the correct date.");
            return false;
        } else {
            $("#search").siblings('span.clientmessage').removeClass('error').text("");
        }

        $("#LoadingImage").show();

        var st_date = `${startDate.getFullYear()}-${padNumber(startDate.getMonth() + 1)}-${padNumber(startDate.getDate())}`;
        var et_date = `${endDate.getFullYear()}-${padNumber(endDate.getMonth() + 1)}-${padNumber(endDate.getDate())}`;
        $http.get(`/quality/lJCVMonitoringSearch?startDate=${st_date}&endDate=${et_date}&parameterName=${p_name}&lotNo=${lotNo}&level=${level}`)
            .then(function(response) {
      $scope.consoleRes = response.data;
      var xAxisData = $scope.consoleRes.xAxisDates;
      var qcLevelData = $scope.consoleRes.qclevel.map(Number);
      $("#LoadingImage").hide();

      // Select the table body element
      let tableBody = document.querySelector('#qcTable tbody');
        let totalCount = 0;
      // Function to format date to dd/mm/yyyy
      function formatDate(dateString) {
          let date = new Date(dateString);
          let day = date.getDate();
          let month = date.getMonth() + 1; // January is 0, so we add 1
          let year = date.getFullYear();

          // Ensure day and month are two digits
          if (day < 10) {
              day = '0' + day;
          }
          if (month < 10) {
              month = '0' + month;
          }
          return `${day}/${month}/${year}`;
      }

      // Loop through the data and create table rows
      for (let i = 0; i < qcLevelData.length; i++) {
          let newRow = tableBody.insertRow();
          let dateCell = newRow.insertCell();
          let heightCell = newRow.insertCell();
          dateCell.textContent = formatDate(xAxisData[i]);
          heightCell.textContent = qcLevelData[i];
           totalCount += qcLevelData[i];

      }
        let mean = totalCount / qcLevelData.length;
         let sumOfSquares = qcLevelData.reduce((sum, value) => sum + Math.pow(value - mean, 2), 0);
                let standardDeviation = Math.sqrt(sumOfSquares / qcLevelData.length);
                let cv = (standardDeviation / mean) * 100;
                $scope.coefOfvarient = cv;

// Add a row for displaying total count
        let totalRow = tableBody.insertRow();
        let totalCell = totalRow.insertCell();
        totalCell.colSpan = 2; // Span across all columns
        totalCell.style.textAlign = 'right'; // Align content to the right
        totalCell.textContent = ` Total Count: ${totalCount} `;

         // Add a row for displaying mean
                let meanRow = tableBody.insertRow();
                let meanCell = meanRow.insertCell();
                meanCell.colSpan = 2; // Span across all columns
                meanCell.style.textAlign = 'right'; // Align content to the right
                meanCell.textContent = `Mean: ${mean.toFixed(2)}`;

                 let sdRow = tableBody.insertRow();
                        let sdCell = sdRow.insertCell();
                        sdCell.colSpan = 2; // Span across all columns
                        sdCell.style.textAlign = 'right'; // Align content to the right
                        sdCell.textContent = `Standard Deviation: ${standardDeviation.toFixed(2)}`;

            });
    };

    function padNumber(num) {
        return (num < 5 ? '0' : '') + num;
    }

    $('#search').on('click', function() {
        $scope.allDetails();
    });
});


