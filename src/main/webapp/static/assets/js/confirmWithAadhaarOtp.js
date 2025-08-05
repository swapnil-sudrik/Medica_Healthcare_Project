var app = angular.module('ngndhm', []).config(function($httpProvider) {
    $httpProvider.defaults.headers.common['Cache-Control'] = 'no-cache';
    $httpProvider.defaults.cache = false;
});


app.controller('searchServicesController', function SearchDoctorController($scope, $http) {
    $scope.consoleRes = {}; // Corrected
    $scope.profileDetailsObject = {}; // Corrected
    $scope.pngDetails;
    $scope.jsonResponse;
    $('#aadharOtp').hide();
    //fetch all details
    $scope.confirmOtp = function() {
        let otp = $('#otp').val();
        let token = $('#token').val();
        if (!otp) {
            $('#otp').focus();
            alert("Please enter OTP before confirm.");
            return false;
        }

        $http.get("/app/confirmWithAadhaarOtp?otp=" + otp + "&token=" + token).then(
            function(response) {
                $scope.consoleRes = response.data;
                $scope.profileDetailsObject = JSON.parse($scope.consoleRes.profileDetails);
                $scope.jsonResponse = $scope.consoleRes.pngDetails;
                // Decode the Base64 encoded SVG content
                var decodedSvgContent = atob($scope.jsonResponse);
                // Create a new DOMParser
                var parser = new DOMParser();
                // Parse the XML string
                var xmlDoc = parser.parseFromString(decodedSvgContent, "text/xml");
                // Extract the SVG content from the XML
                var svgContent = xmlDoc.getElementsByTagName('svg')[0].outerHTML;
                // Modify the SVG content if needed
                var modifiedSvgContent = svgContent.replace('width="', 'width="800"').replace('height="', 'height="400"');
                // Display the SVG content
                document.getElementById('svgContainer').outerHTML = modifiedSvgContent;
                $('#confirm_button').hide();
                $('#otp').hide();
                $('#tableBody').show();
                $('#aadharOtp').show();
            });
    }

    $('#confirm_button').on(
        'click',
        function() {
            $scope.confirmOtp();
        });

});
