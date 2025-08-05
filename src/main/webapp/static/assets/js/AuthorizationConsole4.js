//$("select").select2();


var app = angular.module('nglis', []).config(function($httpProvider) {
	$httpProvider.defaults.headers.common['Cache-Control'] = 'no-cache';
	$httpProvider.defaults.cache = false;
});

app.controller('searchServicesController', function SearchDoctorController(
		$scope, $http) {
	
	$(".impline").hide();
	$(".footerPage").hide();
	
	//find all section list
	$http.get(
			"/ajax/allSectionlist").then(
			function(response) {
				$scope.allSectionlist = response.data;
				//console.log('allSectionlist', $scope.allSectionlist);
			});
	
	//find all department list
	$http.get(
	"/ajax/allDepartmentList").then(
	function(response) {
		$scope.allDepartmentList = response.data;
		//console.log('allSectionlist', $scope.allDepartmentList);
	});
	 
	//find all test list
	$http.get(
	"/ajax/allTestList").then(
	function(response) {
		$scope.allTestList = response.data;
		//console.log('allTestList', $scope.allTestList);
	});

	//getting end date 24 hours on start date 
	$('#starts_at').on(
			'change',
			function() {
				let startAt = new Date($('#starts_at').val());
				startAt.setHours(startAt.getHours() + 24);
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
				// console.log("end", new Date() + $scope.endAt);
			});
	
	//fetch all details
	 $scope.allDetails=function(){ 
		 let s_date = $('#starts_at').val();
			// alert(s_date);
			let e_date = $('#ends_at').val();
			$("#LoadingImage").show();
			$(".impline").hide();
			$http.get(
					"/ajax/authorizationConsole4?starts_at=" + s_date
							+ "&ends_at=" + e_date+ "&shift=" + $("#shift").val()).then(
					function(response) {
						$scope.consoleRes = response.data;
						$(".impline").show();
						$("#LoadingImage").hide();
						
						//page count
						var range = []; 
						for(var i=0; i< $scope.consoleRes[0].totalPages;i++) { 
						   range.push(i); 
						} 
						$scope.range = range;
						
						$(".footerPage").show();
						// alert(response);
						/*console.log("console", $scope.range);
						angular.forEach($scope.consoleRes, function(item) {
							console.log("idddd", item.order.patient.mrn2.toString().padStart(6, '0') )
						});*/
						
						 

					});
	 }

	//on search click
	$('.getResult').on(
			'click',
			function() {
				$scope.allDetails(); 
			});
	
	//on page number click
	$('#shift').on(
			'change',
			function() {
				$scope.allDetails(); 		
			});
	
	//on page  number click
	$scope.nextpagelist = function(n) {
       // alert(n);
        let s_date = $('#starts_at').val();
		// alert(s_date);
		let e_date = $('#ends_at').val();
		$("#LoadingImage").show();
	
		$http.get(
				"/ajax/authorizationConsole4?starts_at=" + s_date
						+ "&ends_at=" + e_date+ "&shift=" + $("#shift").val()+ "&page=" +n).then(
				function(response) {
					$("#LoadingImage").hide();
					$scope.consoleRes = response.data;
					
					//page count
					var range = []; 
					for(var i=0; i< $scope.consoleRes[0].totalPages;i++) { 
					   range.push(i); 
					} 
					$scope.range = range;
					
					$(".footerPage").show();
					// alert(response);
/*					console.log("console", $scope.range);
					angular.forEach($scope.consoleRes, function(item) {
						console.log("idddd", item)
					});*/
				});
    }
	
	
	
	
	
	// sort ordering (Ascending or Descending). Set true for desending
	 $scope.reverse = false; 
	 // called on header click
	 $scope.sortColumn = function(col){
	  $scope.column = col;
	  if($scope.reverse){
	   $scope.reverse = false;
	  }else{
	   $scope.reverse = true;
	  }
	 };
	 
});

//search
function objToString (obj) {
    let str = '';
    for (const [p, val] of Object.entries(obj)) {
        str += `${val}\n`;
    }
    return str;
}

app.filter('searchFor', function(){
	
    // All filters must return a function. The first parameter
    // is the data that is to be filtered, and the second is an
    // argument that may be passed with a colon (searchFor:searchString)
	
    return function(arr, searchString){
    	
            if(!searchString){
                    return arr;
            }

            var results = [];
            
            searchString = searchString.toLowerCase();
            
            // Using the forEach helper method to loop through the array
            angular.forEach(arr, function(item){
            
            var machineStatus="";
            if(item.orderDetail.machineStatus==0){
            //item.orderDetail.machineStatus="Not Received";
            machineStatus="Not Received";
            }else{machineStatus="Received";}
            	
            	let searchableItem = item.orderDetail.investigation.invest_name+" "+
            	item.order.createdAt+" "+item.orderDetail.investigation.department+" "+
            	item.orderDetail.investigation.section+" "+item.orderDetail.status+" "+
            	item.order.patient.mrn1+" "+item.order.patient.mrn2+" "+item.order.patient.title+" "+
            	item.order.patient.firstName +" " +item.order.patient.middleName +" "+
            	item.order.patient.lastName +" "+ item.order.patient.relationWithPatient +" "+
            	item.order.patient.relativeName +" "+ item.order.patient.age +" "+
            	item.order.patient.gender +" "+ item.order.facility +" "+ item.orderDetail.barcode +" "+
            	item.orderDetail.investigation.sampleType +" "+ 	item.orderDetail.testName+" "+ 	machineStatus
            	
            	//console.log(item)
                    if(searchableItem.toLowerCase().indexOf(searchString) !== -1){
                            results.push(item);
                    }

            });

            return results;
    };

});


//search order status 
app.filter('searchForStatus', function(){
	
    // All filters must return a function. The first parameter
    // is the data that is to be filtered, and the second is an
    // argument that may be passed with a colon (searchFor:searchString)
	
    return function(arr, searchString){
    		
            if(!searchString){
                    return arr;
            }

            var results = [];
            
            searchString = searchString.toLowerCase();
            
            // Using the forEach helper method to loop through the array
            angular.forEach(arr, function(item){
            	
            	let searchableItem = item.orderDetail.status;
            	if($('#status').val() == 'PENDING'){
        			if(['VERIFIED', 'RESULT_SAVED', 'SAMPLE_COLLECTED'].indexOf(searchableItem) !== -1){
           			 results.push(item);    		      		                          
                 }
                }else {
                	if(searchableItem.toLowerCase().indexOf(searchString) !== -1){
                        results.push(item);
                        
                	}
                }
                    

            });
            return results;
    };

});
