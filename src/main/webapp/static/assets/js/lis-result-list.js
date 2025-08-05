var app=angular.module('nglis',[]).config(function ($httpProvider) {
    $httpProvider.defaults.headers.common['Cache-Control'] = 'no-cache';
    $httpProvider.defaults.cache = false;
  });

app.controller('searchServicesController', function SearchDoctorController($scope,$http) {
	  $scope.getlist =  function(){	
		  console.log($scope.selectedDate)
		  console.log(document.getElementById('machine_id').value);
		  $scope.machineName = document.getElementById('machine_id').value;
		  console.log($scope.machineName);
		  console.log($scope.statusVeri);
		  console.log(document.getElementById('datefield').value)
		  var select_date = new Date(document.getElementById('datefield').value);
			var dateString1 = select_date.getFullYear() + "-"
					+ (select_date.getMonth() + 1) + "-"
					+ select_date.getDate()
					console.log("hello", select_date);
			$http.get("/ajax/lis-result?today_date="+dateString1+"&sampleId="+$scope.barcode+"&machineName="+$scope.machineName)
	        .then(function(response) {
	        	if(dateString1 == null || dateString1 == "1970-1-1") {
	        		document.getElementById("errMsg").innerHTML = "Please Select Date!!!";
	        	}else {
	        		document.getElementById("errMsg").innerHTML = "";
	        	}
	        	
	        	console.log("Search", dateString1, $scope.barcode, $scope.machineName);
     	        console.log('all details', response.data);
     	        $scope.items = response.data;
     	        angular.forEach($scope.items, function(s) {
     	        	if(s.verifiedStatus == undefined) {
     		 			console.log("NO");
     		 			s.verifiedStatus ="NO";
     		 		}
     	        })
     	       
//     	       if($scope.statusVeri == undefined) {
//     	    	   return 'No';
//     	       }
//     	        	console.log(document.getElementById("veri").value);
//     	        	if(document.getElementById("veri").innerHTML) {
//     	        		return 'NO';
//     	        		veri
//     	        	}
     	  
	        
//	        check box toggle
	        $scope.selecteditem = [];
	        $scope.exist = function(item) {
		        console.log(item);
		        return $scope.selecteditem.indexOf(item) > -1
	        };

	        $scope.toggleSelection = function(item) {
		        console.log("got", item)
		        $scope.selectAll = false
		        let idx = $scope.selecteditem.indexOf(item);
		        if (idx > -1) {
		        $scope.selecteditem.splice(idx, 1);
		        } else {
		        $scope.selecteditem.push(item);
		        console.log("sdasdadsadas", $scope.selecteditem.length)
		        }
	        };

	        $scope.checkAll = function() {
	        	console.log('checked', !$scope.selectAll);
	        	if(!$scope.selectAll) {
	        		console.log('yes');
		        	angular.forEach($scope.items, function(item) {
			            let idx = $scope.selecteditem.indexOf(item);
			            if(idx >= 0){
//			            	return true;
			            	console.log("fasuhfohfdoa",true);
				        } else {
//				        	$scope.selecteditem.push(item);
				        	console.log('all', item)
			        		console.log($scope.selecteditem.push(item), $scope.selecteditem.length);
				        }
			        })
	        	}
	        	else {
//	        		$scope.selecteditem = [];
	        		console.log('safasfdada', $scope.selecteditem = [])
	        	}
	        }

	        $scope.save = function() {
		        angular.forEach($scope.selecteditem, function(i) {
		        	 $http.post('/ajax/lis-result-verified-all?id='+i.id)
     		 	.then(function(response) {	
     		 			i.verifiedStatus = "YES";
     		        console.log("afterEdit", response);
     		   }); 
		        })
	        }


	        
	       }); 		        
      }
       /* $http.get("/ajax/lis-result?today_date="+$scope.selectedDate)
        .then(function(response) {
        console.log(response.data);
        $scope.items = response.data;
        });   */ 
        
        var list = $scope;
        list.serviceList = [];  
        
        $scope.getInfo = function(iv_id, patientId){
        	$scope.getId =  patientId;
	        $scope.get_iv = iv_id;
        	$http.get("/ajax/lis-test-details?iv_id="+$scope.get_iv)
	        .then(function(response) {
	        console.log("hello2", iv_id);
	        console.log(response.data);
	        $scope.details = response.data;	
	        $scope.getId =  patientId;
	        $scope.get_iv = iv_id;
	        console.log($scope.getId, $scope.get_iv);
	        }); 
        	$http.get("/ajax/patient-details?patientId="+$scope.getId)
	        .then(function(responsetwo) {
	        console.log("hello3", patientId);
	        console.log(responsetwo.data);
	        $scope.userdetails = responsetwo.data;	
	        $scope.getAge = function(birthday){
	            var birthday = new Date(birthday);
	            var today = new Date();
	            var age = ((today - birthday) / (31557600000));
	            var age = Math.floor( age );
	            return `${age} years / `;
	          }
	        }); 
        	
        }
        	$scope.setSelectedValue = function(value) {
        		let prevalue;
        		$scope.prevalue = value
        		console.log('previous Value', $scope.prevalue.result );
        	}
        	
        	$scope.updateResultValue = function() {
        		 console.log("detail", $scope.prevalue.result, $scope.prevalue.id);
        		 $http.post('/ajax/lis-result-update?id='+$scope.prevalue.id+"&editValue="+$scope.prevalue.result)
        		 .then(function(response) {
        		        console.log("afterEdit", response);
        		        console.log('lolololololo', $scope.details.result);
        		        console.log($scope.prevalue.orderDetail.id, $scope.prevalue.orderDetail.patientId);
               		 $scope.getInfo($scope.prevalue.orderDetail.id, $scope.prevalue.orderDetail.patientId);
        		   });
        		}
//        	$scope.setstatus = function(status) {
//        		let prestatus;
//        		$scope.prestatus = status;
//        		console.log('preStatus', prestatus);
//        	}
//        	
//        	$scope.statusvalue = function() {
//        		 console.log("status new new", $scope.prestatus.id, $scope.prestatus.verifiedStatus);
//        		$http.post('/ajax/lis-result-verified?id='+$scope.prestatus.id)
//       		 	.then(function(response) {	
//       		 		$scope.prestatus.verifiedStatus = "YES";
//       		        console.log("afterEdit", response);
//       		   }); 
//        	}
        	
        	
        	

	});

		
$(document)
.ready(
		function() {
			 $("#machine_id").select2();
			 $("#datefield").pickadate({
				 max: new Date(),
				 format: 'yyyy-mm-dd',
			 });
		});



