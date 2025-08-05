
var app = angular.module('nglis', []);
app.config(function($httpProvider) {
    $httpProvider.defaults.headers.common['Cache-Control'] = 'no-cache';
    $httpProvider.defaults.cache = false;
});
app.controller('SearchServicesController', function($scope, $http) {
    $scope.consoleRes = [];
    $scope.searchDetails = function() {
        if ($scope.barcode.length < 10) {
            alert("Barcode must be 10 characters long.");
            return;
        } else {
            $http.get("/sampleTrackingDetails?barcode=" + $scope.barcode)
                .then(function(response) {
                    $scope.consoleRes = response.data;
                })
                .catch(function(error) {
                    console.error('Error fetching data:', error);
                });
        }
    };
$scope.formatDate = function(dateString) {
    if (!dateString) return ''; // Handle empty or undefined date string
    var date = new Date(dateString);
    var options = {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        timeZoneName: 'short'
    };
    return date.toLocaleDateString('en-IN', options);
    }
});
