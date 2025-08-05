
/* Author Deepshikha Rajput

 city List Retrieve 
*/

$("#state").on("change",function(e){
	$("#citylist").empty(); 
	
	var state = $("#state").val();
	
	var hosp_state = $("#hosp_state").val(state);
	 $.ajax({
	        type: "GET",
	        url: "/city-list",
	        data: "state=" +hosp_state.val() ,
	        dataType: "html",
	        success: function (data)
	        {
	        	
	        	var city_detail =  JSON.parse(data);
	        	var cnt = city_detail.length;
	        	
	        	for(var i=0;i<cnt;i++){
	        	//alert(city_detail[i].cityName);
	        	
	        	var a ='<option value="'+city_detail[i].cityName+'">'+city_detail[i].cityName+'</option>';
	        	$("#citylist").append(a);
	        
	        }
	        	
	            
	           
	            
	            
	        }
	    });
	
	
});
$(document).ready(function(){


var hosp_state = $("#hosp_state").val();

var state = $("#state").val(hosp_state);
$("#state").select2({ width: '100%'	});

$("#citylist").select2({ width: '100%'	});

});



