//10 days filter in all report
let startAt;
let e_month, e_date, e_hours, e_min;
let s_month;
let endAt;
let today;

$('.startcheck10').on(
			'change',
			function() {
				startAt = new Date($('.startcheck10').val());
				today = new Date();
				if(startAt.getDate() == today.getDate() && startAt.getMonth() == today.getMonth() && startAt.getFullYear() == today.getFullYear()) {
					//do nothing;
				}else {
					$(".endcheck10").val(" ");
				}
				
			});

$('.endcheck10').on(
			'change',
			function() {
				//alert("fdfds");
				startAt = new Date($('.startcheck10').val());
				startAt.setDate(startAt.getDate() + 9);
				//alert(startAt);
				
				s_month = startAt.getMonth() + 1;
				endAt = new Date($('.endcheck10').val());
				
			
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
				
				let dateis = new Date(startAt.getFullYear() + '-' + e_month + '-'
						+ e_date + ' ' +  startAt.getHours() + ':' +  startAt.getMinutes());
				//alert(endAt + '=============' + dateis);
				if(endAt > dateis) { 
					//console.log("yes", endAt, dateis);
					$(".endcheck10").val(" ");
					$(".endcheck10").siblings('span.clientmessage').text("* End date should not be more than 10 days from start date.");
				}else {
					$(".endcheck1").siblings('span.clientmessage').text(" ");
					//console.log("do nothing", endAt, dateis);
				}

			});