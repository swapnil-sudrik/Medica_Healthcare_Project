window.onload = function() {

var pcArr =[]
$.ajax({
    type: "GET",
    url: "/public/total-patient-districtwise",	
    async: false,
    success: function (data)
    {	 
    	console.log(data)
    	data.map((obj)=>pcArr.push({y:obj.totalTestCount, label:obj.labDistrictName}))
       
    }
});
var chart = new CanvasJS.Chart("investigationContainer", {
	theme: "light2", // "light1", "light2", "dark1", "dark2"
	exportEnabled: true,
	animationEnabled: true,
	title: {
		text: "Investigations Performed",
		fontWeight: "normal",
	},
	data: [{
		type: "pie",
		startAngle: 25,
		toolTipContent: "<b>{label}</b>: {y}%",
		/*showInLegend: "true",
		legendText: "{label}",
		indexLabelFontSize: 16,
		indexLabel: "{label} - {y}%",*/
		indexLabel: "#percent%",
		percentFormatString: "#0.##",
		toolTipContent: "{y} (#percent%)",
		indexLabelFontSize: 16,
		indexLabel: "{label} - #percent%",
		dataPoints: pcArr
	}]
});
	chart.render();
//================================================================================================================
	// Best Performing Location - Today SOF
	var todayBestLocation = '';
	var labName0=0,labName1=0,labName2=0,labName3=0,labName4=0,labName5=0,labName6=0,labName7=0,labName8=0,labName9=0;
	var labValue0=0,labValue1=0,labValue2=0,labValue3=0,labValue4=0,labValue5=0,labValue6=0,labValue7=0,labValue8=0,labValue9=0;
	
	$.ajax({
	    type: "GET",
	    url: "/public/today-best-location",	
	    async: false,
	    success: function (data)
	    {	 
	    	todayBestLocation = data;
	       
	    }
	});
	
	var str = '';
	for(var i=0; i< todayBestLocation.length;i++){
		var arr0 = todayBestLocation[0][1].split("-");
		labName0 = arr0[1];
		labValue0 = todayBestLocation[0][0];
		
		var arr1 = todayBestLocation[1][1].split("-");
		labName1 = arr1[1];
		labValue1 = todayBestLocation[1][0];
		
		var arr2 = todayBestLocation[2][1].split("-");
		labName2 = arr2[1];
		labValue2 = todayBestLocation[2][0];
		
		var arr3 = todayBestLocation[3][1].split("-");
		labName3 = arr3[1];
		labValue3 = todayBestLocation[3][0];
		
		var arr4 = todayBestLocation[4][1].split("-");
		labName4 = arr4[1];
		labValue4 = todayBestLocation[4][0];
	}
	
	var chart = new CanvasJS.Chart("bestPerformLocationTodayContainer", {
		animationEnabled: true,
		theme: "light2", // "light1", "light2", "dark1", "dark2"
		title: {
			text: "Best Performing Location - Today (Tests)",
			fontWeight: "normal",
		},
		/*axisY: {
			title: "Growth Rate (in %)",
			suffix: "%"
		},
		axisX: {
			title: "Locations"
		},*/
		data: [{
			type: "column",
			//yValueFormatString: "#,##0.0#\"%\"",
			yValueFormatString: "#,##0.0#\"\"",
			dataPoints: [
				{ label: labName0, y: (labValue0 === null) ? 0 : labValue0 },	
				{ label: labName1, y: (labValue1 === null) ? 0 : labValue1 },
				{ label: labName2, y: (labValue2 === null) ? 0 : labValue2 },
				{ label: labName3, y: (labValue3 === null) ? 0 : labValue3 },
				{ label: labName4, y: (labValue4 === null) ? 0 : labValue4 },
				
			]
		}]
	});
	chart.render();
	// Best Performing Location - Today EOF
//================================================================================================================
	// Best Performing Location - Overall SOF
	var overallBestLocation = '';
	var labName0=0,labName1=0,labName2=0,labName3=0,labName4=0,labName5=0,labName6=0,labName7=0,labName8=0,labName9=0;
	var labValue0=0,labValue1=0,labValue2=0,labValue3=0,labValue4=0,labValue5=0,labValue6=0,labValue7=0,labValue8=0,labValue9=0;
	
	$.ajax({
	    type: "GET",
	    url: "/public/overll-best-location",	
	    async: false,
	    success: function (data)
	    {	 
	    	overallBestLocation = data;
	       
	    }
	});
	
	var str = '';
	for(var i=0; i< overallBestLocation.length;i++){
		var arr0 = overallBestLocation[0][1].split("-");
		labName0 = arr0[1];
		labValue0 = overallBestLocation[0][0];
		
		var arr1 = overallBestLocation[1][1].split("-");
		labName1 = arr1[1];
		labValue1 = overallBestLocation[1][0];
		
		var arr2 = overallBestLocation[2][1].split("-");
		labName2 = arr2[1];
		labValue2 = overallBestLocation[2][0];
		
		var arr3 = overallBestLocation[3][1].split("-");
		labName3 = arr3[1];
		labValue3 = overallBestLocation[3][0];
		
		var arr4 = overallBestLocation[4][1].split("-");
		labName4 = arr4[1];
		labValue4 = overallBestLocation[4][0];
	}
	
	var chart = new CanvasJS.Chart("bestPerformLocationContainer", {
		animationEnabled: true,
		theme: "light2", // "light1", "light2", "dark1", "dark2"
		title: {
			text: "Best Performing Location - Overall (Tests)",
			fontWeight: "normal",
		},
		/*axisY: {
			title: "Growth Rate (in %)",
			suffix: "%"
		},
		axisX: {
			title: "Locations"
		},*/
		data: [{
			type: "column",
			yValueFormatString: "#,##0.0#\"\"",
			dataPoints: [
				{ label: labName0, y: labValue0 },	
				{ label: labName1, y: labValue1 },
				{ label: labName2, y: labValue2 },
				{ label: labName3, y: labValue3 },
				{ label: labName4, y: labValue4 },
				
			]
		}]
	});
	chart.render();
	// Best Performing Location - Overall EOF
//================================================================================================================
	
//===============Machine-wise Tests SOF=================================================================================================
	var pcArr =[]
	$.ajax({
	    type: "GET",
	    url: "/public/machine-wise-test",	
	    async: false,
	    success: function (data)
	    {	 
	    	console.log(data)
	    	data.map((obj)=>pcArr.push({y:obj.totalMachineTest, label:obj.deptName}))
	       
	    }
	});
	var chart = new CanvasJS.Chart("machineContainer", {
		animationEnabled: true,
		title:{
			text: "Machine / Department",
			horizontalAlign: "center",
			fontWeight: "normal",
			fontSize: 22,
		},
		data: [{
			type: "doughnut",
			startAngle: 60,
			//innerRadius: 60,
			indexLabelFontSize: 13,
			indexLabel: "{label} - #percent%",
			toolTipContent: "<b>{label}:</b> {y} (#percent%)",
			dataPoints: pcArr
		}]
	});
	chart.render();
//=====================Machine-wise Tests EOF===========================================================================================

}// END window.onload
/*========================================= TOP 10 INVESTIGATION SOF ===========================================*/
$(document).ready(function(){
	var invName0=0,invName1=0,invName2=0,invName3=0,invName4=0,invName5=0,invName6=0,invName7=0,invName8=0,invName9=0;
	var invValue0=0,invValue1=0,invValue2=0,invValue3=0,invValue4=0,invValue5=0,invValue6=0,invValue7=0,invValue8=0,invValue9=0;
	
	var top10invest = "";
	$.ajax({
	    type: "GET",
	    url: "/public/topteninvestigation",	
	    async: false,
	    success: function (data)
	    {	 
	    	top10invest = data;
	       
	    }
	});
	console.log(top10invest);
	//var topArr = [];
	//top10invest.map((data)=>topArr.push({label:data.}))
	var str = '';
	
	for(var i=0; i< top10invest.length;i++){							
		str += "{ label:" + top10invest[i][1] + ", y:" + top10invest[i][0] + "},";
		
		invName0 = top10invest[0][1];
		invValue0 = top10invest[0][0];
		
		invName1 = top10invest[1][1];
		invValue1 = top10invest[1][0];
		
		invName2 = top10invest[2][1];
		invValue2 = top10invest[2][0];
		
		invName3 = top10invest[3][1];
		invValue3 = top10invest[3][0];
		
		invName4 = top10invest[4][1];
		invValue4 = top10invest[4][0];
		
		invName5 = top10invest[5][1];
		invValue5 = top10invest[5][0];
		
		invName6 = top10invest[6][1];
		invValue6 = top10invest[6][0];
		
		invName7 = top10invest[7][1];
		invValue7 = top10invest[7][0];
		
		invName8 = top10invest[8][1];
		invValue8 = top10invest[8][0];
		
		invName9 = top10invest[9][1];
		invValue9 = top10invest[9][0];
	}
	var chart = new CanvasJS.Chart("mostInvestigationContainer", {
		  theme: "light1", // "light1", "light2", "dark1"
		  animationEnabled: true,
		  exportEnabled: true,
		  title: {
		    text: "Top 10 Investigation",
		    fontWeight: "lighter",
		  },
		  axisX: {
		    margin: 10,
		    labelPlacement: "inside",
		    tickPlacement: "inside"
		  },
		  axisY2: {
		    //title: "Views (in thousand)",
		    titleFontSize: 14,
		    includeZero: true,
		    //suffix: "k"
		  },
		  data: [{
		    type: "bar",
		    axisYType: "secondary",
		    yValueFormatString: "#,###.##",
		    indexLabel: "{y}",
		    color: "LightSeaGreen",
		    dataPoints: [
		    	{ label: invName9, y: invValue9 },
		    	{ label: invName8, y: invValue8 },
		    	{ label: invName7, y: invValue7 },
		    	{ label: invName6, y: invValue6 },
		    	{ label: invName5, y: invValue5 },
		    	{ label: invName4, y: invValue4 },
		    	{ label: invName3, y: invValue3 },
		    	{ label: invName2, y: invValue2 },
		    	{ label: invName1, y: invValue1 },
		    	{ label: invName0, y: invValue0 },
		    ]
		  }]
		});
		chart.render();
});
/*========================================= TOP 10 INVESTIGATION EOF ===========================================*/
/*========================================= PATIENT SERVED SOF =================================================*/
$(document).ready(function(){
	var pcArr =[]
	$.ajax({
	    type: "GET",
	    url: "/public/total-patient-districtwise",	
	    async: false,
	    success: function (data)
	    {	 
	    	console.log(data)
	    	data.map((obj)=>pcArr.push({y:obj.totalPatient, label:obj.labDistrictName}))
	       
	    }
	});
	

console.log(pcArr)
var chart = new CanvasJS.Chart("patientContainer", {
	theme: "light2", // "light1", "light2", "dark1", "dark2"
	exportEnabled: true,
	animationEnabled: true,
	title: {
		text: "Patients Served",
		fontWeight: "normal",
	},
	data: [{
		type: "pie",
		startAngle: 25,
		toolTipContent: "<b>{label}</b>: {y}%",
		/*showInLegend: "true",
		legendText: "{label}",
		indexLabelFontSize: 16,
		indexLabel: "{label} - {y}%",*/
		indexLabel: "#percent%",
		percentFormatString: "#0.##",
		toolTipContent: "{y} (#percent%)",
		indexLabelFontSize: 16,
		indexLabel: "{label} - #percent%",
		dataPoints: pcArr
			/*[
			{ y: 51.08, label: "Bhopal" },
			{ y: 27.34, label: "Gwalior" },
			{ y: 10.62, label: "Jabalpur" },
			{ y: 5.02, label: "Vidisha" },
			{ y: 4.07, label: "Hoshangabad" },
			{ y: 8.22, label: "Khandwa" },
			{ y: 12.44, label: "Satna" }
		]*/
	}]
});
chart.render();
});
/*========================================= PATIENT SERVED EOF =================================================*/