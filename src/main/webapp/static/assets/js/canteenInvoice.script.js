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
   
  	var header = document.getElementById('additemDetail');
	var btns = header.getElementsByClassName("rowitem");
	var h = (btns.length) + 1;
	var i_name = $('option:selected', $("#item")).attr('subvalue');
	var i_id = $('option:selected', $("#item")).attr('value');
	var rate = $('option:selected', $("#item")).attr('rate');
	var qty = $("#qty").val();
	var total = $("#total").val();
	
	if((i_name!='') && (qty!='') && (i_name!= null)){
		count += 1;
	$("#additemDetail").append('<tr class="rowitem" id="rowitem'+h+'"><td>'+count+'</td>'+
	'<td>'+i_name+'<input type="hidden" value="'+i_id+'" name="i_id"></td>'+
	'<td align="right">'+rate+'<input type="hidden" value="'+rate+'" name="rate"></td>'+
	'<td align="right">'+qty+'<input type="hidden" value="'+qty+'" name="qty"></td>'+
	'<td align="right">'+(total)+'<input type="hidden" value="'+total+'" name="total"></td>'+
	'<td><span class="text-danger" onclick="removeRow('+h+')"><i class="nav-icon i-Close-Window font-weight-bold"></i></span></td></tr>');
		
	
	 $("#item").val('');
	 $("#rate").val('');
	 $("#total").val('');
	$("#add").siblings('span.clientmessage').removeClass('error');
    $("#add").siblings('span.clientmessage').text("");
    subtotal(h);
    if(btns.length >= 1){
    	 
    	 $("#item option[subvalue='"+i_name+"']").remove();
    }else{
	} }else{
		$("#add").siblings('span.clientmessage').addClass('error');
        $("#add").siblings('span.clientmessage').text("Please fill all information.");
	}
};

let removeRow = function (index) {

	
	let a = $("#rowitem"+index).find("td").eq(1).text(); 
	let b = $("#rowitem"+index).find("td:eq(2) input[type='hidden']").val();
	let d = $("#rowitem"+index).find("td").eq(2).text(); 
	let c = '<option value="'+b+'" subvalue="'+a+'" rate="'+d+'">'+a+'</option>';
	$("#item").append(c);
	$("#rowitem"+index).remove();
	var header = document.getElementById('additemDetail');
	var btns = header.getElementsByClassName("rowitem");
	var h = (btns.length) + 1;
	 subtotal(h);
    
	
	
};

$("#item").on("change",function(e){
	var rate  = $('option:selected', $("#item")).attr('rate');
	$("#rate").val(rate);
	
});
let totalamoutrow = function () {

	var a = $("#rate").val();
	var b =	$("#qty").val();
	var total = Number(a*b);
	$("#total").val(total.toFixed(2));
};

let subtotal = function (h) {
	var sum_total = 0;
	//alert(h);
	for(var i = 1; i<(h+1); i++  ){
	//	alert(h+'==='+i);
	var x = $("#rowitem"+i).find("td").eq(4).text(); 
	sum_total += Number(x);
	}
	$("#total_amount").html(sum_total.toFixed(2));
	$("input[name='total_amount']").val(sum_total.toFixed(2));
	

};
