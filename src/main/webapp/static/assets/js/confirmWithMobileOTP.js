var app = angular.module('ngndhm', []).config(function($httpProvider) {
    $httpProvider.defaults.headers.common['Cache-Control'] = 'no-cache';
    $httpProvider.defaults.cache = false;
});


app.controller('searchServicesController', function SearchDoctorController($scope, $http) {
    $scope.consoleRes = {}; // Corrected
    $scope.profileDetailsObject = {}; // Corrected
    $scope.pngDetails;
    $scope.jsonResponse;
    $('#mobileOtp').hide();
    //fetch all details
    $scope.confirmOtp = function() {
        let otp = $('#otp').val();
        let token = $('#token').val();
        if (!otp) {
            $('#otp').focus();
            alert("Please enter OTP before confirm.");
            return false;
        }

        $http.get("/app/confirmWithMobileOTP?otp=" + otp + "&token=" + token).then(
            function(response) {
                $scope.consoleRes = response.data;
                $scope.profileDetailsObject = JSON.parse($scope.consoleRes.profileDetails);
                $scope.jsonResponse = $scope.consoleRes.pngDetails;

                // Extract the SVG content from the JSON
                $scope.svgIndex = $scope.jsonResponse.indexOf('<svg');
                if($scope.svgIndex>0){
                 $scope.svgContent = $scope.jsonResponse.substring($scope.svgIndex);
                $scope.modifiedSvgContent = $scope.svgContent.replace('<svg', '<svg width="800" height="400"');
                // Display the SVG content
                document.getElementById('svgContainer').outerHTML =$scope.modifiedSvgContent;

                } else{
                    // Decode the Base64 encoded SVG content
                    $scope.decodedSvgContent = atob($scope.jsonResponse);
                    // Create a new DOMParser
                    $scope.parser = new DOMParser();
                    // Parse the XML string
                    $scope.xmlDoc = $scope.parser.parseFromString($scope.decodedSvgContent, "text/xml");
                    // Extract the SVG content from the XML
                    $scope.svgContent = $scope.xmlDoc.getElementsByTagName('svg')[0].outerHTML;
                    // Modify the SVG content if needed
                    $scope.modifiedSvgContent = $scope.svgContent.replace('width="', 'width="800"').replace('height="', 'height="400"');
                    // Display the SVG content
                    document.getElementById('svgContainer').outerHTML = $scope.modifiedSvgContent;
                }
                $('#confirm_button').hide();
                $('#otp').hide();
                $('#tableBody').show();
                $('#mobileOtp').show();
            });
    }

    $('#confirm_button').on(
        'click',
        function() {
            $scope.confirmOtp();
        });

});
