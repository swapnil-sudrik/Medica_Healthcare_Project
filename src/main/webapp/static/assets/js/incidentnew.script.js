/* Author Deepshikha Rajput  
 * Date- 07/03/2020
 * */
$(document).ready(function() {
	//alert($('#subName').val());

	$('.timepickerStartTime').pickatime({
		format: 'HH:i:00',
		min: new Date().getHours() + ":" + new Date().getMinutes(),
		max: new Date().getHours() + ":" + new Date().getMinutes(),
	});
	$('.timepickerEndTime').pickatime({
		format: 'HH:i:00',
		min: "00:00",
		max: new Date().getHours() + ":" + new Date().getMinutes(),
	});
	$('.datepickerStartDate').pickadate({
		format: 'yyyy-mm-dd',
		max: new Date(),
	});
	$('.datepickerEndDate').pickadate({
		format: 'yyyy-mm-dd',
	});
	notBreakdown();
	HourCalculate();
	//resonChange();
	//equipChange();


});

$("#incident_name").click(function() {

	resonChange();

});
$("#subName").click(function() {


	notBreakdown();
});

// Map your choices to your option value
var lookup = {
	'Breakdown': ['Please select an option', 'Equipment', 'Software', 'Hardware', 'Infrastructure', 'Manpower'],
	'Quality': ['Please select an option', 'EQAS', 'Departmental QC'],
	'Inventory': ['Please select an option', 'Stockout'],
	'Control': ['Please select an option', 'Control Not Available', 'Expired Control', 'Other'],
	'Calibration': ['Please select an option', 'Calibration'],



};

// When an option is changed, search the above for matching choices
let resonChange = function() {

	// Set selected option as variable
	var selectValue = $("#incident_name").val();
	var subName = $('#subName').val();
	var s = "";
	// Empty the target field
	$('#subName').empty();

	// For each choice in the selected option
	for (i = 0; i < lookup[selectValue].length; i++) {
		if ((lookup[selectValue][i] === subName)) {
			var s = "SELECTED";
		}
		// Output choice in the target field
		$('#subName').append(
			"<option " + s + " value='" + lookup[selectValue][i] + "'>"
			+ lookup[selectValue][i] + "</option>");

		s = "";
	}
}

//Map your choices to your option value
var lookup2 = {
	//'Equipment' : [ 'AdviaCentaur_CP', 'BA_400','Biorad_D10','Stago_SatelliteMax', 'Sysmex_XN550','UriScan_Pro' ],
	'Equipment': ['SWELABALFA_BM850', 'NV50_Serial', 'SDBiosensorF200'],
	'Software': ['HUBSPOKE'],
	'Hardware': ['Monitor-server', 'Server', 'Node CPU', 'Switch', 'LAN cable', 'RJ-45 connector', 'Serial Cable', 'Camera', 'Keyboard', 'Mouse', 'Printer', 'DVR', 'UPS'],
	'Infrastructure': ['Furniture', 'Civil work', 'Electrical', 'Others'],
	'Manpower': ['Manpower'],
	'Inventory': ['SWELABALFA_BM850', 'NV50_Serial', 'SDBiosensorF200'],
	'EQAS': ['SWELABALFA_BM850', 'NV50_Serial', 'SDBiosensorF200'],
	'Control Not Available': ['AdviaCentaur_CP', 'BA_400', 'Biorad_D10', 'Stago_SatelliteMax', 'Sysmex_XN550', 'UriScan_Pro'],
	'Expired Control': ['AdviaCentaur_CP', 'BA_400', 'Biorad_D10', 'Stago_SatelliteMax', 'Sysmex_XN550', 'UriScan_Pro'],
	'Other': ['AdviaCentaur_CP', 'BA_400', 'Biorad_D10', 'Stago_SatelliteMax', 'Sysmex_XN550', 'UriScan_Pro'],
	'Calibration': ['AdviaCentaur_CP', 'BA_400', 'Biorad_D10', 'Stago_SatelliteMax', 'Sysmex_XN550', 'UriScan_Pro'],
	'Departmental QC': ['SWELABALFA_BM850', 'NV50_Serial', 'SDBiosensorF200'],



};

// When an option is changed, search the above for matching choices
let equipChange = function() {

	// Set selected option as variable
	var selectValue2 = $("#subName").val();
	// Empty the target field
	$('#equipmentName').empty();

	// For each choice in the selected option
	for (i = 0; i < lookup2[selectValue2].length; i++) {
		// Output choice in the target field
		$('#equipmentName').append(
			"<option value='" + lookup2[selectValue2][i] + "'>"
			+ lookup2[selectValue2][i] + "</option>");
	}
}
$("#subName").click(function() {

	equipChange();
});


let notBreakdown = function() {

	var selectValue3 = $("#subName").val();
	//alert(selectValue3);
	if (selectValue3 == 'Equipment') {
		$(".offence").hide();
		$(".parameter_count").hide();

		$(".duration").show();
		$(".reason").show();
		//$(".remarkbrekdown").show();
		//$(".remarequas").hide();
		//$("#remarkbrekdown").prop("disabled", false);
		//$("#remarequas").prop("disabled", true);
		//$(".startDateTime").find('label').text("Start Date & Time");
		//$(".endDateTime").find('label').text("End Date & Time");
		$(".deductionAmount").show();
	} else if (selectValue3 == 'EQAS') {

		$(".duration").hide();
		$(".reason").hide();
		$(".offence").show();
		$(".parameter_count").show();
		//$(".remarkbrekdown").hide();
		//$(".remarequas").show();
		//$("#remarkbrekdown").prop("disabled", true);
		//$("#remarequas").prop("disabled", false);
		//	$(".startDateTime").find('label').text("Equas Date & Time");
		//	$(".endDateTime").find('label').text("Equas Report Date & Time");
		$(".deductionAmount").show();
	} else if (selectValue3 == 'Stockout') {
		$(".offence").hide();
		$(".parameter_count").hide();

		$(".duration").show();
		$(".reason").show();

		$(".deductionAmount").show();

	} else {
		$(".offence").hide();
		$(".parameter_count").hide();
		$(".duration").hide();
		$(".deductionAmount").hide();
		$(".reason").hide();
	}

}
let HourCalculate = function() {
	var selectValue3 = $("#subName").val();
	//alert(selectValue3);
	if (selectValue3 == 'Equipment') {
		var f_date = $("#startDate").val();
		var f_time = $("#startTime").val();
		var s_date = $("#endDate").val();
		var s_time = $("#endTime").val();
		var n_statdt = new Date(f_date + ' ' + f_time);
		var n_enddt = new Date(s_date + ' ' + s_time);
		if (f_date != '' && f_time != '' && s_date != '' && s_time != '') {
			if (n_statdt.getTime() < n_enddt.getTime()) {
				var diff = (n_statdt.getTime() - n_enddt.getTime()) / (1000 * 60 * 60);
				//alert(Math.abs(Math.round(diff)));
				$("#duration").val(Math.abs(Math.round(diff)));
				if (Math.abs(Math.round(diff)) > 72) {
					var d2 = Math.abs(Math.round(diff)) - 72;
					var d3 = d2 / 24;
					var res = d3.toString().split(".");
					var d5 = res[0];
					var d6 = res[1];
					if (d6 > 0) {
						d5 = Number(d5) + 1;
					}
					//alert(d3+' ****'+' ****'+d5+' ****'+d6);
					$("#deductionAmount").val(Math.round(d5) * 2000);
				} else {
					$("#deductionAmount").val(0);
				}
				$(".endDateTimeerror").removeClass('error');
				$(".endDateTimeerror").text("");
			} else {
				$("#endDate").val('');
				$("#endTime").val('');
				$(".endDateTimeerror").addClass('error');
				$(".endDateTimeerror").text("Please Select Correct Date and Time.");
			}
		}
	} else if (selectValue3 == 'EQAS') {
		$("#deductionAmount").val(0);
		var e1 = $('option:selected', $("#parameter_count")).val();
		var e2 = $('option:selected', $("#offence")).val();
		var e3 = 1000;
		var e4 = 0;
		if (e2 == '1') {
			e4 = e1 * e3;
		} else if (e2 == '2') {
			e4 = e1 * (2 * e3);
		} else {
			e4 = 0;
		}
		$("#deductionAmount").val(e4);
	} else if (selectValue3 == 'Stockout') {
		var f_date = $("#startDate").val();
		var f_time = $("#startTime").val();
		var s_date = $("#endDate").val();
		var s_time = $("#endTime").val();
		var n_statdt = new Date(f_date + ' ' + f_time);
		var n_enddt = new Date(s_date + ' ' + s_time);
		if (f_date != '' && f_time != '' && s_date != '' && s_time != '') {
			if (n_statdt.getTime() < n_enddt.getTime()) {
				var diff = (n_statdt.getTime() - n_enddt.getTime()) / (500 * 60 * 60);
				//alert(Math.abs(Math.round(diff)));
				$("#duration").val(Math.abs(Math.round(diff)));
				if (Math.abs(Math.round(diff)) > 72) {
					var d2 = Math.abs(Math.round(diff)) - 72;
					var d3 = d2 / 24;
					var res = d3.toString().split(".");
					var d5 = res[0];
					var d6 = res[1];
					if (d6 > 0) {
						d5 = Number(d5) + 1;
					}
					//alert(d3+' ****'+' ****'+d5+' ****'+d6);
					$("#deductionAmount").val(Math.round(d5) * 500);
				} else {
					$("#deductionAmount").val(0);
				}
				$(".endDateTimeerror").removeClass('error');
				$(".endDateTimeerror").text("");
			} else {
				$("#endDate").val('');
				$("#endTime").val('');
				$(".endDateTimeerror").addClass('error');
				$(".endDateTimeerror").text("Please Select Correct Date and Time.");
			}
		}
	}




	else {
		$("#deductionAmount").val(0);
	}


}


//date validation based on selection incident and subtype
let startAt, e_month, e_date, e_hours, e_min, s_month, today, dateis, finalDate;
function dateFormate(dateNo) {
	//alert(dateNo);
	startAt = new Date();
	startAt.setDate(startAt.getDate() - dateNo);
	s_month = startAt.getMonth() + 1;

	if (s_month.toLocaleString().length <= 1) {
		e_month = '0' + (startAt.getMonth() + 1);

	} else {
		e_month = (startAt.getMonth() + 1);
	}
	if (startAt.getDate().toLocaleString().length <= 1) {
		e_date = '0' + (startAt.getDate());
		//alert(e_date);
	} else {
		e_date = (startAt.getDate());
		//alert(e_date);
	}

	dateis = startAt.getFullYear() + '-' + e_month + '-'
		+ e_date;

	//today = new Date().getFullYear() + '-' + e_month + '-' + new Date().getDate();
	//console.log("========================================="+new Date(today));
	//	finalDate = dateis.toLocaleDateString("en-US", { day: 'numeric' })
	//	+ " " +
	//	dateis.toLocaleDateString("en-US", { month: 'long' })
	//	+ ", " +
	//	dateis.toLocaleDateString("en-US", { year: 'numeric' });


	//console.log(e_date);
}

function todayDateFormate() {
	//alert(dateNo);
	startAt = new Date();
	//startAt.setDate(startAt.getDate() - dateNo);
	s_month = startAt.getMonth() + 1;

	if (s_month.toLocaleString().length <= 1) {
		e_month = '0' + (startAt.getMonth() + 1);
	} else {
		e_month = (startAt.getMonth() + 1);
	}
	if (startAt.getDate().toLocaleString().length <= 1) {
		e_date = '0' + (startAt.getDate());
		//alert(e_date);
	} else {
		e_date = (startAt.getDate());
		//alert(e_date);
	}

	today = new Date().getFullYear() + '-' + e_month + '-' + e_date;
	//console.log("========================================="+new Date(today));
	//	finalDate = dateis.toLocaleDateString("en-US", { day: 'numeric' })
	//	+ " " +
	//	dateis.toLocaleDateString("en-US", { month: 'long' })
	//	+ ", " +
	//	dateis.toLocaleDateString("en-US", { year: 'numeric' });


	//console.log(e_date);
}



$('.datepickerStartDate').on(
	'change',
	function() {

		//console.log($("#subName").val() + "----" + $('#incident_name').val());

		if ($('#incident_name').val() == "Inventory") {
			//console.log($('#incident_name').val());
			dateFormate(1);
			todayDateFormate();
			//alert($('.datepickerStartDate').val() + "---------" + finalDate);
			if ($('.datepickerStartDate').val() != dateis && $('.datepickerStartDate').val() != today) {
				alert("Please selet date before 24 hours for Inventory!!");
				$('.datepickerStartDate').val("");
			}
		} else if ($("#incident_name").val() == "Breakdown") {
			console.log(new Date($('.datepickerStartDate').val()));
			dateFormate(2);
			todayDateFormate();
			if (!(new Date($('.datepickerStartDate').val()) >= new Date(dateis) && new Date($('.datepickerStartDate').val()) <= new Date(today))) {
				alert("Please selet date before 48 hours for Breakdown!!");
				$('.datepickerStartDate').val("");
			}
		} else {
			//do nothing
		}
	});

$('.datepickerEndDate').on(
	'change',
	function() {
		//console.log(new Date($("#end_date").val()) + "----" + $('#incident_name').val());
		if ($('#incident_name').val() == "Inventory") {
			//	console.log(finalDate);
			dateFormate(1);
			//alert($('#end_date').val() + "-----" + finalDate + "----" + $('#start_date').val());
			if (new Date($('.datepickerEndDate').val()) < new Date(dateis)) {
				alert("End Date cannot be prior to startdate!!");
				$('.datepickerEndDate').val("");
			}
		} else if ($("#incident_name").val() == "Breakdown") {
			//console.log($("#subName").val());
			dateFormate(2);
			if (new Date($('.datepickerEndDate').val()) < new Date(dateis)) {
				alert("End Date cannot be prior to startdate!!");
				$('.datepickerEndDate').val("");
			}
		} else {
			//do nothing
		}
	});


// Function for select incident before Date

function selectIncidentFirst() {
	// alert();
	// $('.datepickerStartDate').val("");
	//  $('.datepickerEndDate').val("");

}
selectIncidentFirst();


resonChange();