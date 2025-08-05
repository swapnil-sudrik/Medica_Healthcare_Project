$("#verify").click(function() {
	//alert($("#verify").val());
	if ($("#verify").prop("checked")) {
		if ($("#verify").val() != 0) {
			$("#verifyButton").show();
		}
	} else {
		$("#verifyButton").hide();
	}
});
$("#verifyButton")
		.click(
				function() {
					$("#verifyButton").hide();
					//alert($(".verifySelected").val());
					var start = $(".start_date").val();
					var end = $(".end_date").val();

					var startDate = new Date(start.replace(/-/g, "/"));
					var endDate = new Date(end.replace(/-/g, "/"));

					if ($(".start_date").val() == '') {

						$(".start_date").siblings('span.clientmessage')
								.addClass('error');
						$(".start_date").siblings('span.clientmessage').text(
								"Please select the start date.");
						$(".start_date").css('border-color', '#F5A9A9');
						$(".start_date").focus();
						return false;
					} else if ($(".end_date").val() == '') {

						$(".end_date").siblings('span.clientmessage').addClass(
								'error');
						$(".end_date").siblings('span.clientmessage').text(
								"Please select the end date.");
						$(".end_date").css('border-color', '#F5A9A9');
						document.getElementsByClassName("end_date").focus();
						return false;
					} else if (startDate > endDate) {
						$("#search").siblings('span.clientmessage').addClass(
								'error');
						$("#search").siblings('span.clientmessage').text(
								"Please select the correct date.");
						return false;

					} else {
						$("#search").siblings('span.clientmessage')
								.removeClass('error');
						$("#search").siblings('span.clientmessage').text("");
					}

					$("#LoadingImage").show();

					//CHANGE DATE FORMATE
					var st_date = new Date(start);
					var et_date = new Date(end);

					var dateString1 = st_date.getFullYear() + "-"
							+ (st_date.getMonth() + 1) + "-"
							+ st_date.getDate()
					var dateString2 = et_date.getFullYear() + "-"
							+ (et_date.getMonth() + 1) + "-"
							+ et_date.getDate()
					var selected = new Array();
					var chks = document
							.getElementsByClassName("verifySelected");

					for (var i = 0; i < chks.length; i++) {
						if (chks[i].checked) {
							selected.push(chks[i].value);
						}
					}
					if (selected.length > 0) {
						selected.join(",");
					}

					$.ajax({
						type : "GET",
						url : "/testWiseReportUpdate",
						data : "im_id=" + selected + "&start_date="
								+ dateString1 + "&end_date=" + dateString2,
						dataType : "html",
						success : function(data) {

							$("#LoadingImage").hide();
							$(".verify_status").siblings('span.clientmessage')
									.text("Selected Reports Verified.");
							$(".verify_status").css('font-color', '#30B225');
							$("#verifyButton").hide();
							$("#search").click();

						}
					});

				});

$(document).ready(function() {

	// Initialize select2
	$("#invest_ids").select2({
		width : '300px'
	});

	// Read selected option

	$("#sd").pickadate({
		max : new Date(),
		clear : ''
	});
	$("#ed").pickadate({
		max : new Date(),
		clear : ''
	});
	var d = new Date();
	if ($('#sd').val() == '') {
		$('#sd').pickadate('picker').set('select', d, {
			format : 'YYYY-MM-DDTHH:mm:ss. sssZ'
		}).trigger("change");

	}
	if ($('#ed').val() == '') {
		$('#ed').pickadate('picker').set('select', d, {
			format : 'YYYY-MM-DDTHH:mm:ss. sssZ'
		}).trigger("change");
	}

});
$("#search")
		.click(
				function() {
				$("#search_data").empty();
					var start = $(".start_date").val();
					var end = $(".end_date").val();

					var startDate = new Date(start.replace(/-/g, "/"));
					var endDate = new Date(end.replace(/-/g, "/"));

					if ($(".start_date").val() == '') {

						$(".start_date").siblings('span.clientmessage')
								.addClass('error');
						$(".start_date").siblings('span.clientmessage').text(
								"Please select the start date.");
						$(".start_date").css('border-color', '#F5A9A9');
						$(".start_date").focus();
						return false;
					} else if ($(".end_date").val() == '') {

						$(".end_date").siblings('span.clientmessage').addClass(
								'error');
						$(".end_date").siblings('span.clientmessage').text(
								"Please select the end date.");
						$(".end_date").css('border-color', '#F5A9A9');
						document.getElementsByClassName("end_date").focus();
						return false;
					} else if (startDate > endDate) {
						$("#search").siblings('span.clientmessage').addClass(
								'error');
						$("#search").siblings('span.clientmessage').text(
								"Please select the correct date.");
						return false;

					} else {
						$("#search").siblings('span.clientmessage')
								.removeClass('error');
						$("#search").siblings('span.clientmessage').text("");
					}

					$("#LoadingImage").show();
					//CHANGE DATE FORMATE

					var st_date = new Date(start);
					var et_date = new Date(end);

					var dateString1 = st_date.getFullYear() + "-"
							+ (st_date.getMonth() + 1) + "-"
							+ st_date.getDate()
					var dateString2 = et_date.getFullYear() + "-"
							+ (et_date.getMonth() + 1) + "-"
							+ et_date.getDate()
					$.ajax({
						type : "GET",
						url : "/testWiseReportSearch",
						data : "im_id=" + $("#invest_ids").val()
								+ "&start_date=" + dateString1 + "&end_date="
								+ dateString2,
						dataType : "html",
						success : function(data) {

							$("#LoadingImage").hide();
							$(".default").empty();
							
							$("#search_data").html(data);
						}
					});
				});

$(".detailsearch").click(
		function() {
			var start = $(".start_date").val();
			var end = $(".end_date").val();
			var im_id = $(this).val();
			var st_date = new Date(start);
			var et_date = new Date(end);

			var dateString1 = st_date.getFullYear() + "-"
					+ (st_date.getMonth() + 1) + "-" + st_date.getDate()
			var dateString2 = et_date.getFullYear() + "-"
					+ (et_date.getMonth() + 1) + "-" + et_date.getDate()
			
					var url = "/verificationReportDetail?im_id=" + im_id + "&start_date="
							+ dateString1 + "&end_date=" + dateString2+ "&status=Reports Authorized";
			 window.open(url, '_self');

		});

$(document)
		.ready(
				function() {
					$('#verify').on('click', function() {
						if (this.checked) {
							$('.verifySelected').each(function() {
								this.checked = true;
							});
						} else {
							$('.verifySelected').each(function() {
								this.checked = false;
							});
						}
					});

					$('.verifySelected')
							.on(
									'click',
									function() {
										var total = 0;
										if ($('.verifySelected:checked').length == $('.verifySelected').length) {
											$('#verify').prop('checked', true);
										} else {
											$('#verify').prop('checked', false);
										}
									});
					$('.verifySelected').on('click', function() {
						var total = 0;
						var chks = document
						.getElementsByClassName("verifySelected");

				for (var i = 0; i < chks.length; i++) {
					if (chks[i].checked) {
						var checked_data = (chks[i].getAttribute("data-value"));
						total += Number(checked_data);
						
						
					}
				}
				if (total != 0) {
					$("#verifyButton").show();
				
			} else {
				$("#verifyButton").hide();
			}

					});
				});
