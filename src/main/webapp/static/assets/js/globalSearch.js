var app=angular.module('ngbilling',[]).config(function ($httpProvider) {
    $httpProvider.defaults.headers.common['Cache-Control'] = 'no-cache';
    $httpProvider.defaults.cache = false;
  });

app.directive('ngEnter', function () {
    return function (scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
            if (event.which === 13) {
                scope.$apply(function () {
                    scope.$eval(attrs.ngEnter);
                });

                event.preventDefault();
            }
        });
    };
});
app.controller('searchServicesController',function SearchDoctorController($scope,$http) {
//    var search= document.getElementById("lastName").value;  
    // On loading list.blade OR lab-list directly without previous search fetch services
    	// Mapped in InvestigationController
        $http.get("/rates-json")
        .then(function(response) {
        	console.log(response.data);
        $scope.items = response.data;
        });    
        
        var list = $scope;
        list.serviceList = [];       
        
        $scope.getTotal = function(){
        	var total_amount = 0;
        	for(var i=0; i< list.serviceList.length; i++){
        		var order = list.serviceList[i];
        		total_amount = total_amount+order.rate;
        	}
        	return total_amount;
        }
        
        $scope.getCount = function(){        	
        	return list.serviceList.length;
        }
        
        function Service(rateId, testName, rate,  qnty){
        	this.rateId =rateId;
        	this.testname= testName;
        	this.rate = rate;
        	this.qnty = "1";
        }
        
        $scope.update =  function(i){
//        	list.serviceList.push(new Service(i.id, i.investigation.invest_name, i.rate, 1));    
        	list.serviceList.push(i);
//        	alert(JSON.stringify(i));
        	$("button[name="+i[1]+"]").attr('disabled',true);
        	$("#searchtest").val("");
        	$('#searchtest').focus();        
//        	alert($("button[name="+i.id+"]").attr("name"));
//        	$("button[name="+i.id+"]").parent().toggleClass('open');
        	/*var userTxt= this.selService;        	
        	$("#tests").find("option").each(function(){
        		if($(this).val().toUpperCase()==userTxt){
        			list.serviceList.push(JSON.parse($(this).attr('data-attr'))); 
        			$(this).attr('disabled','disabled');
        			$("input[name='test']").val('');
        		}
        	});*/
        };

        $scope.updateDiscount= function(elm){
        	var elm_obj = document.getElementById(elm.target.id);            	
        	var valid=parseFloat(elm_obj.value.match(/^-?\d*(\.)?\d*?$/)); 
        	if(valid!=null && !isNaN(valid)){
            	var rate = parseFloat(elm_obj.getAttribute('data-rate'));
            	var input = parseFloat(elm_obj.value);
            	if(rate<input){
            		elm_obj.value=0; 
            		swal({
                        type: 'error',
                        title: 'Enter amount less than '+rate,
                        text: 'Select appropriate discount rights',
                        confirmButtonText: 'Dismiss',
                        buttonsStyling: false,
                        confirmButtonClass: 'btn btn-lg btn-danger'
                    });            		
            		} else{
            			var total=0;
    	            	var x = document.getElementsByName("disc");
    	            	for(var i=0; i< x.length; i++){        
    	            		total += parseInt(x[i].value);
    	            	}
//    	            	document.getElementById("totalDiscount").value=total;
    	            	$scope.getTotalDiscount();
            		} 
            	
        	}else{        		
        		elm_obj.value=0;   
        		swal({
    	            type: 'warning',
    	            title: 'Warning!',
    	            text: 'Please enter valid discount in number',
    	            buttonsStyling: false,
    	            confirmButtonClass: 'btn btn-lg btn-warning'
    	        });
        	} 
        	
        }
        
        $scope.getTotalDiscount = function(){
        	var total=0;
        	var x = document.getElementsByName("disc");
        	for(var i=0; i< x.length; i++){        		
        		total += parseFloat(x[i].value);
        	}

        	return total;        	
        }
        
        $scope.getPayment = function(){
        	//alert(parseFloat($("#totalDiscount").val()));
        	var pay = $scope.getTotal() - $scope.getTotalDiscount();
        	return pay;        	
        }
        
        $scope.setDiscount = function(){
        	var x = document.getElementsByName("disc");
        	var y = document.getElementsByName("rate");
        	for(var i=0; i< x.length; i++){        		
        		x[i].value=y[i].value*$scope.discount/100;
        	}
        }
        
        $scope.removeService = function(index){
        	$("button[name="+list.serviceList[index].id+"]").attr('disabled',false);
        	list.serviceList.splice(index, 1);
        }
        
});          
     
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
            	
                    if(item[2].toLowerCase().indexOf(searchString) !== -1){
                            results.push(item);
                    }

            });

            return results;
    };

});

/*$("#referredDepartment").on("change",function(e){
	
	$("#referredBy").empty(); 
	var referredDepartment  = $('option:selected', $("#referredDepartment")).attr('subvalue');
	
	 $.ajax({
	        type: "GET",
	        url: "/findRefferdBy",
	        data: "referredDepartment=" + referredDepartment,
	        dataType: "html",
	        success: function (data)
	        { 
	        	
	        	var details =  JSON.parse(data);
	        	var cnt = details.length;
	        	$("#referredBy").append('<option selected value="SELF">SELF</option>');
	        	for(var i=0;i<cnt;i++){
	        	
	        	var a = '<option value="'+details[i].referredByName.toUpperCase()+'">'+details[i].referredByName.toUpperCase()+'</option>';
	        //	alert(a);
	        		$("#referredBy").append(a);
	        		
	       
	       
	        }
	    }
	
	
});
});         */