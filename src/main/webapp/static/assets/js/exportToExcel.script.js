$(".btnExport").click(function (e) {
	var txt = $('.ExportData').find("table").html();
	var txt2 = "<table border=1>"+txt+"</table>";
	//alert("<table>"+txt+"</table>");
	
    window.open('data:application/vnd.ms-excel,' + encodeURIComponent(txt2));
    //e.preventDefault();
});