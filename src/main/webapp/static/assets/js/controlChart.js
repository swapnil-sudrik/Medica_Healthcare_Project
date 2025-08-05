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
    $scope.qcSdVal = "";

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

        $http.get(`/quality/controlValueChartSearch?startDate=${st_date}&endDate=${et_date}&parameterName=${p_name}&lotNo=${lotNo}&level=${level}`)
            .then(function(response) {
                $scope.consoleRes = response.data;

                if ($scope.consoleRes && $scope.consoleRes.xAxisDates) {
                    // Convert date strings to Date objects
                    $scope.consoleRes.xAxisDates = $scope.consoleRes.xAxisDates.map(dateString => new Date(dateString));
                }
                var qcMapMean = $scope.consoleRes.mean;
                var qcSd = $scope.consoleRes.sd;
                $scope.qcMapMeanVal = qcMapMean;
                $scope.qcSdVal = qcSd;
                var xAxisData = $scope.consoleRes.xAxisDates;
                var yAxisData = $scope.consoleRes.yAxisCal;
                var qcLevelData = $scope.consoleRes.qclevel.map(Number);
                $scope.renderChart(xAxisData, qcLevelData, yAxisData);

                $("#LoadingImage").hide();
                $('#search').prop('disabled', false).text('Search');
            });
    };

    function padNumber(num) {
        return (num < 5 ? '0' : '') + num;
    }

    $('#search').on('click', function() {
        $scope.allDetails();
    });
$scope.renderChart = function(xAxisData, qcLevelData, yAxisData) {
    var yAxisOptionsLeft = {};
    var yAxisOptionsRight = {};
    var intervals = [];
    var dataPoints = [];
    var istDateStr = '';
    if (Array.isArray(yAxisData)) {
        const parsedYAxisData = yAxisData.map(parseFloat);
        const minValue = Math.min(...parsedYAxisData);
        const maxValue = Math.max(...parsedYAxisData);
        const meanValue = parsedYAxisData[0];
        const sdValue = parsedYAxisData[1];

        // Define intervals
        intervals = [
            meanValue,
            meanValue + sdValue,
            meanValue + 2 * sdValue,
            meanValue + 3 * sdValue,
            meanValue - sdValue,
            meanValue - 2 * sdValue,
            meanValue - 3 * sdValue
        ];

        // Set Y-Axis options for the left side
        yAxisOptionsLeft = {
            minimum: meanValue - 4 * sdValue,
            maximum: Math.max(maxValue, meanValue + 4 * sdValue),
            interval: sdValue, // Ensure intervals match SD
            stripLines: [
                { value: meanValue, label: `Mean: ${meanValue.toFixed(2)}`, labelFontColor: "blue", lineDashType: "solid", color: "blue", lineThickness: 2, labelAlign: "near", labelPlacement: "outside" },
                { value: meanValue + sdValue, label: `+1SD: ${(meanValue + sdValue).toFixed(2)}`, labelFontColor: "green", lineDashType: "dash", color: "green", lineThickness: 2, labelAlign: "near", labelPlacement: "outside" },
                { value: meanValue + 2 * sdValue, label: `+2SD: ${(meanValue + 2 * sdValue).toFixed(2)}`, labelFontColor: "green", lineDashType: "dash", color: "green", lineThickness: 2, labelAlign: "near", labelPlacement: "outside" },
                { value: meanValue + 3 * sdValue, label: `+3SD: ${(meanValue + 3 * sdValue).toFixed(2)}`, labelFontColor: "red", lineDashType: "dash", color: "red", lineThickness: 2, labelAlign: "near", labelPlacement: "outside" },
                { value: meanValue - sdValue, label: `-1SD: ${(meanValue - sdValue).toFixed(2)}`, labelFontColor: "green", lineDashType: "dash", color: "green", lineThickness: 2, labelAlign: "near", labelPlacement: "outside" },
                { value: meanValue - 2 * sdValue, label: `-2SD: ${(meanValue - 2 * sdValue).toFixed(2)}`, labelFontColor: "green", lineDashType: "dash", color: "green", lineThickness: 2, labelAlign: "near", labelPlacement: "outside" },
                { value: meanValue - 3 * sdValue, label: `-3SD: ${(meanValue - 3 * sdValue).toFixed(2)}`, labelFontColor: "red", lineDashType: "dash", color: "red", lineThickness: 2, labelAlign: "near", labelPlacement: "outside" }
            ],
            gridThickness: 0,
            includeZero: false,
            lineThickness: 0,
            labelFontColor: "#000",
            tickLength: 10,
            tickColor: "#000",
            title: '',
            labelFormatter: function(e) {
                // Return an empty string if the value is not a significant point
                return intervals.includes(e.value) ? e.value.toFixed(2) : "";
            }
        };

        // Set Y-Axis options for the right side
        yAxisOptionsRight = {
            includeZero: true,
            minimum: minValue,
            maximum: maxValue,
            gridThickness: 0,
            includeZero: false,
            lineColor: "#C0C0C0",
            labelFontColor: "#C0C0C0",
            tickLength: 10,
            tickColor: "#C0C0C0",
            titleFontSize: 12,
            titleFontColor: "#C0C0C0",
            lineThickness: 2,
            title: "Data Values",
            opposite: true
        };

        // Determine color for each data point
        dataPoints = xAxisData.map((xValue, index) => {
            let yValue = qcLevelData[index];
            let color = "green"; // Default color
            if (yValue > meanValue + 2 * sdValue || yValue < meanValue - 2 * sdValue) {
                color = "red";
            }

            return {
                x: xValue,
                y: yValue,
                color: color
            };
        });
    }

var qcAvDate = $scope.consoleRes.meanExpiryDate;
var dateObj = new Date(qcAvDate);
dateObj.setMinutes(dateObj.getMinutes() + 330);
var istFormattedDate = dateObj.toLocaleDateString('en-GB', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
});

    if (intervals.length > 0) {
        intervals.forEach(value => console.log(value.toFixed(2)));
    }

    var chart = new CanvasJS.Chart("chartContainer", {
        title: { text: "LJ Graph" },
        axisX: {
            valueFormatString: "DD/MM",
            interval: 1,
            intervalType: "day"
        },
        axisY: yAxisOptionsLeft,
        axisY2: yAxisOptionsRight,
        data: [{
            type: "line",
            connectNullData: true,
            xValueType: "dateTime",
            xValueFormatString: "DD MMM hh:mm TT",
            dataPoints: dataPoints
        }],
        toolTip: {
               content: function(e) {
                   function formatDate1(dateStr) {
                       const date = new Date(dateStr);
                       return date.toISOString().split('T')[0];
                   }
                   function formatDate2(dateStr) {
                       const [day, month, year] = dateStr.split('/');
                       return `${year}-${month}-${day}`;
                   }
                   const formattedDate1 = formatDate1((e.entries[0].dataPoint.x));
                   const formattedDate2 = formatDate2(istFormattedDate);

                   var content = "Date: " + CanvasJS.formatDate(e.entries[0].dataPoint.x, "DD MMM hh:mm TT") + "<br/>";
                   content += "Value: " + e.entries[0].dataPoint.y + "<br/>";
                   if(formattedDate1 == formattedDate2){
                   content += "Mean: " + $scope.qcMapMeanVal + "<br/>";
                   content += "Sd: " +$scope.qcSdVal + "<br/>"
                    }
                   if (e.entries[0].dataPoint.customData) {
                       content += "Custom Data: " + e.entries[0].dataPoint.customData + "<br/>";
                   }
                   return content;
               }
        }
    });

    chart.render();
};

});


