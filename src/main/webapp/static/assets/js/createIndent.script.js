/* Author Deepshikha Rajput */
$(document).ready(function(){
	 $("select").select2();
	 $(window).resize(function(){
  // Initialize select2
  $("select").select2();
    // Read selected option
	 });
});
count = 0;
let addRow = function () {
	//alert();
   
  	var header = document.getElementById('addproductDetail');
	var btns = header.getElementsByClassName("rowitem");
	var h = (btns.length) + 1;
	var sub_cat = $("#sub_cat").val();
	var p_name = $("#int_id").val();
	var p_name_option  = $('option:selected', $("#int_id")).attr('subvalue');
	var r_stock = $("#r_stock").val();
	var a_stock = $("#a_stock").val();
	var priority = $("#priority").val();
	var ro_level = $("#ro_level").val();
	var remark = $("#remark").val();
	if((sub_cat!='') && (p_name!='')  && (r_stock!='')){
		count += 1;
	$("#addproductDetail").append('<tr class="rowitem" id="rowitem'+h+'"><td>'+count+'</td>'+
	'<td>'+sub_cat+'<input type="hidden" value="'+sub_cat+'" name="sub_cat"></td>'+
	'<td>'+p_name_option+'<input type="hidden" value="'+p_name+'" name="int_id"></td>'+
	'<td>'+a_stock+'<input type="hidden" value="'+a_stock+'" name="a_stock"></td>'+
	'<td>'+ro_level+'<input type="hidden" value="'+ro_level+'" name="ro_level"></td>'+
	'<td>'+r_stock+'<input type="hidden" value="'+r_stock+'" name="r_stock"></td>'+
	'<td>'+priority+'<input type="hidden" value="'+priority+'" name="priority"></td>'+
	'<td>'+remark+'<input type="hidden" value="'+remark+'" name="remark"></td>'+
	'<td><span class="text-danger" onclick="removeRow('+h+')"><i class="nav-icon i-Close-Window font-weight-bold"></i></span></td></tr>');
    
	 $("#int_id").val('');
	 $("#a_stock").val('');
	 $("#ro_level").val('');
	$("#add").siblings('span.clientmessage').removeClass('error');
    $("#add").siblings('span.clientmessage').text("");
    if(btns.length >= 1){
    	// $("#sub_cat").prop('disabled', 'disabled');
    	 $("#priority").prop('disabled', 'disabled');
    	 $("#int_id option[subvalue='"+p_name_option+"']").remove();
    }else{
    	// $("#sub_cat").removeAttr("disabled");
    }
	}else{
		$("#add").siblings('span.clientmessage').addClass('error');
        $("#add").siblings('span.clientmessage').text("Please fill all information.");
	}
};

let removeRow = function (index) {
	//alert(index);
	let a = $("#rowitem"+index).find("td").eq(2).text(); 
	let b = $("#rowitem"+index).find("td:eq(2) input[type='hidden']").val();
	let d = $("#rowitem"+index).find("td").eq(6).text(); 
	let c = '<option value="'+b+'" subvalue="'+a+'" rolevel="'+d+'">'+a+'</option>';
	$("#int_id").append(c);
	$("#rowitem"+index).remove();
    
	
	
};

$("#sub_cat").on("change",function(e){
	
	$("#int_id").empty(); 
	var sub_id  = $('option:selected', $("#sub_cat")).attr('subvalue');
	
	 $.ajax({
	        type: "GET",
	        url: "/inventory/pruductListCategorywise",
	        data: "sub_cat=" + sub_id,
	        dataType: "html",
	        success: function (data)
	        { 
	        	
	        	var details =  JSON.parse(data);
	        	var cnt = details.length;
	        	$("#int_id").append('<option selected value="">Select</option>');
	        	for(var i=0;i<cnt;i++){
	        	
	        	var a = '<option value="'+details[i].internationalId+'" subvalue="'+details[i].productName+'" rolevel="'+details[i].bufferQty+'">'+details[i].productName+' - '+details[i].packing+'</option>';
	        //	alert(a);
	        		$("#int_id").append(a);
	        		
	       
	       
	        }
	    }
	
	
});
});
$("#int_id").on("change",function(e){
	
	var int_id = $("#int_id").val();
	var p_name_rolevel  = $('option:selected', $("#int_id")).attr('rolevel');
	
	 $.ajax({
	        type: "GET",
	        url: "/inventory/availablestock",
	        data: "int_id=" + int_id,
	        dataType: "html",
	        success: function (data)
	        {
	        	
	        	var a_stock =  JSON.parse(data);
	        	//alert(data);
	        	if(a_stock.length != '0'){
	        		//alert(a_stock.length);
	        		$("#a_stock").val(a_stock.current_qty);
	        		$("#ro_level").val(a_stock.bufferQty);
	        	}
	        	else{
	        		
	        		$("#a_stock").val('0');
	        		$("#ro_level").val('0');
	        	}
	        }
	    });
	
	
});