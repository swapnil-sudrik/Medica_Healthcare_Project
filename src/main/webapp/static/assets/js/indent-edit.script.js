/* Author Deepshikha Rajput */
$(document).ready(function(){
	 $("select").select2({width:'150px'});
	 
	 
    
    
     
	 
/*
var cnt1 = document.getElementsByClassName("sub_cat").length+1;

for(var ind=1;ind<cnt1;){
//alert(ind);
//$("#int_id"+ind).empty(); 
	var sub_id  = $('option:selected', $("#sub_cat"+ind)).attr('subvalue');
	
	 $.ajax({
	        type: "GET",
	        url: "/inventory/pruductListCategorywise",
	        data: "sub_cat=" + sub_id,
	        dataType: "html",
	        success: function (data)
	        { alert(ind);
	        	
	        	var details =  JSON.parse(data);
	        	var cnt = details.length;
	        	//$("#int_id"+index).append('<option selected value="">Select</option>');
	        	for(var i=0;i<cnt;i++){
	        	
	        	var a = '<option value="'+details[i].internationalId+'" subvalue="'+details[i].productName+'" rolevel="'+details[i].bufferQty+'">'+details[i].productName+' - '+details[i].packing+'</option>';
	        //	alert(a);
	        		$("#int_id"+ind).append(a);
	        		
	       
	       
	        }
	    }
	
	
});
}*/
});
count = document.getElementsByClassName("rowitem").length;
let addRow = function () {
	//alert();
   
  	var header = document.getElementById('addproductDetail');
	var btns = header.getElementsByClassName("rowitem");
	var h = (btns.length) + 1;
	
	
		count += 1;
	$("#addproductDetail").append('<tr class="rowitem" id="rowitem'+count+'"><td>'+count+'</td>'+
	'<td><select   name="sub_cat" class="cat_sub" id="sub_cat'+count+'" onchange="newProduct('+count+')" required> </select></td>'+
	'<td><select  name="int_id" id="int_id'+count+'" onchange="newProductchange('+count+')" required><option value="">select<option> <select></td>'+
	'<td  align="right"><input style=" width:70px" type="text"  name="a_stock" id="a_stock'+count+'" required readonly></td>'+
	'<td  align="right"><input style=" width:70px" type="text"  name="ro_level" id="ro_level'+count+'" required readonly></td>'+
	'<td align="right"><input style=" width:70px" type="number" class="number"  name="required_stock" required ></td>'+
	
	'<td><textarea rows="1" name="remark"> </textarea></td>'+
	'<td><span class="text-danger" onclick="removeRow('+count+')"><i class="nav-icon i-Close-Window font-weight-bold"></i></span></td></tr>');
    
	
	$("#sub_cat"+count).append(aopt1);
	$("select").select2({width:'150px'});
};

let removeRow = function (index) {
	
	$("#rowitem"+index).remove();
    
	
	
};

let newProduct = function (index) {

	
	$("#int_id"+index).empty(); 
	var sub_id  = $('option:selected', $("#sub_cat"+index)).attr('subvalue');
	
	 $.ajax({
	        type: "GET",
	        url: "/inventory/pruductListCategorywise",
	        data: "sub_cat=" + sub_id,
	        dataType: "html",
	        success: function (data)
	        { 
	        	
	        	var details =  JSON.parse(data);
	        	var cnt = details.length;
	        	//$("#int_id"+index).append('<option selected value="">Select</option>');
	        	for(var i=0;i<cnt;i++){
	        	
	        	var a = '<option value="'+details[i].internationalId+'" subvalue="'+details[i].productName+'" rolevel="'+details[i].bufferQty+'">'+details[i].productName+' - '+details[i].packing+'</option>';
	        //	alert(a);
	        		$("#int_id"+index).append(a);
	        		
	       
	       
	        }
	    }
	
	
});
}
let newProductchange = function (index) {
	
	var int_id = $("#int_id"+index).val();
	
	var p_name_rolevel  = $('option:selected', $("#int_id"+index)).attr('rolevel');
	
	 $.ajax({
	        type: "GET",
	        url: "/inventory/availablestock",
	        data: "int_id=" + int_id,
	        dataType: "html",
	        success: function (data)
	        {
	        	
	        	var a_stock =  JSON.parse(data);
	        	//alert(a_stock);
	        	if(a_stock.length != '0'){
	        		//alert(a_stock.length);
	        		$("#a_stock"+index).val(a_stock.current_qty);
	        		$("#ro_level"+index).val(a_stock.bufferQty);
	        	}
	        	else{
	        		
	        		$("#a_stock"+index).val('0');
	        		$("#ro_level"+index).val('0');
	        	}
	        }
	    });

}
