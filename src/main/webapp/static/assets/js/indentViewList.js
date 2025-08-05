
$("#search").click(function () {
		
		var status = $("#status").val();
	   
	  
		var start = $("#start_date").val();
	    var end = $("#end_date").val();

	    var startDate = new Date(start.replace(/-/g, "/"));
	    var endDate = new Date(end.replace(/-/g, "/"));

		
	    if ($("#start_date").val() == '') {

	        $("#start_date").siblings('span.clientmessage').addClass('error');
	        $("#start_date").siblings('span.clientmessage').text("Please select the start date.");
	        $("#start_date").css('border-color', '#F5A9A9');
	        $("#start_date").focus();
	        return false;
	    } else if ($(".end_date").val() == '') {

	        $("#end_date").siblings('span.clientmessage').addClass('error');
	        $("#end_date").siblings('span.clientmessage').text("Please select the end date.");
	        $("#end_date").css('border-color', '#F5A9A9');
	        document.getElementsByClassName("end_date").focus();
	        return false;
	    }else if(startDate > endDate){
	    	$("#search").siblings('span.clientmessage').addClass('error');
	        $("#search").siblings('span.clientmessage').text("Please select the correct date.");
	        return false;
	    	
	    }else{
	    	$("#search").siblings('span.clientmessage').removeClass('error');
	        $("#search").siblings('span.clientmessage').text("");
	    }
	    
	    
	    $("#LoadingImage").show();
	    //CHANGE DATE FORMATE
	    var st_date = new Date(start);
	     var et_date = new Date(end);
	     
	    var dateString1 = st_date.getFullYear()  + "-" + (st_date.getMonth() + 1) + "-" + st_date.getDate()
	    var dateString2 = et_date.getFullYear()  + "-" + (et_date.getMonth() + 1) + "-" + et_date.getDate()

	    $("#LoadingImage").show();
	   
	    $.ajax({
	        type: "GET",
	        url: "/inventory/requestedIndentsSearch",
	        data: "start_date=" + dateString1+ "&end_date=" + dateString2+ "&status=" + status,
	        dataType: "html",
	        success: function (data)
	        {

	            $("#LoadingImage").hide();
	            $(".default").hide();
	            $("#search_detail").html(data);
	        }
	    });
	
	
	});
	$("#status").change(function(event){
		
			$("#search").click();
	
	});
	
	$(document).ready(function(){
		//alert();
		$("#start_date").pickadate({max: new Date()});
		$("#end_date").pickadate({max: new Date()});
	});
	

