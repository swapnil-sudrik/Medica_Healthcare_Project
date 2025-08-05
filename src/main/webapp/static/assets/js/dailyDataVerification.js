       $(document).ready(function(){
         $("#investIds").select2({width:'150px'});
          $("#startDate").pickadate({max: new Date()});
            $("#endDate").pickadate({max: new Date()});
        });
        $("#search").click(function () {
            var start = $(".startDate").val();
            var end = $(".endDate").val();

            var startDate = new Date(start.replace(/-/g, "/"));
            var endDate = new Date(end.replace(/-/g, "/"));


            if ($(".startDate").val() == '') {

                $(".startDate").siblings('span.clientmessage').addClass('error');
                $(".startDate").siblings('span.clientmessage').text("Please select the start date.");
                $(".startDate").css('border-color', '#F5A9A9');
                $(".startDate").focus();
                return false;
            } else if ($(".end_date").val() == '') {
                $(".endDate").siblings('span.clientmessage').addClass('error');
                $(".endDate").siblings('span.clientmessage').text("Please select the end date.");
                $(".endDate").css('border-color', '#F5A9A9');
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
            $.ajax({
                type: "GET",
                url: "/dailyDataVerificationReportSearch",
                data: "imId=" + $("#investIds").val() + "&startDate=" + dateString1 + "&endDate=" + dateString2 ,
                dataType: "html",
                success: function (data)
                {
                    $("#LoadingImage").hide();
                    $(".default").hide();
                    $("#search_data").html(data);
                }
            });
        });
//open pending verification report
function openNewTab() {
       var start = $(".startDate").val();
       var end = $(".endDate").val();
       var st_date = new Date(start);
       var et_date = new Date(end);

       var StartDateString = st_date.getFullYear() + "-" + (st_date.getMonth() + 1) + "-" + st_date.getDate();
       var endDateString = et_date.getFullYear() + "-" + (et_date.getMonth() + 1) + "-" + et_date.getDate();
       var url = "/dailyDataVerificationReportNew?startDate=" + StartDateString + "&endDate=" + endDateString ;
       window.open(url, '_blank');
   }

 function startDateChange() {
     let startAt = new Date($('#startDate').val());
     startAt.setDate(startAt.getDate() + 10); // Set end date ten days after start date
     let e_month, e_date;
     let s_month = startAt.getMonth() + 1;
     if (s_month < 10) {
         e_month = '0' + s_month;
     } else {
         e_month = s_month.toString();
     }
     if (startAt.getDate() < 10) {
         e_date = '0' + startAt.getDate();
     } else {
         e_date = startAt.getDate().toString();
     }
     let dateis = startAt.getFullYear() + '-' + e_month + '-' + e_date;
     document.getElementById("endDate").value = dateis;
 }
  function endDateChange() {
      let endAt = new Date($('#endDate').val());
      endAt.setDate(endAt.getDate() - 10); // Set end date ten days after start date
      let s_month, s_date;
      let e_month = endAt.getMonth() + 1;
      if (e_month < 10) {
          s_month = '0' + e_month;
      } else {
          s_month = e_month.toString();
      }
      if (endAt.getDate() < 10) {
          s_date = '0' + endAt.getDate();
      } else {
          s_date = endAt.getDate().toString();
      }
      let dateis = endAt.getFullYear() + '-' + s_month + '-' + s_date;
      document.getElementById("startDate").value = dateis;
  }

$(document).ready(function() {
 var now = new Date();
      var month = (now.getMonth() + 1);
      var day = now.getDate();
      if (month < 10)
          month = "0" + month;
      if (day < 10) {
          day = "0" + day;
      }
      var today = now.getFullYear() + '-' + month + '-' + day;
      $('#startDate').val(today);
    var endDate = new Date(today); // Clone the start date
    endDate.setDate(endDate.getDate()+1); // Set end date one day after start date

    var e_month = endDate.getMonth() + 1 < 10 ? '0' + (endDate.getMonth() + 1) : (endDate.getMonth() + 1);
    var e_date = endDate.getDate() < 10 ? '0' + endDate.getDate() : endDate.getDate();
    var dateis = endDate.getFullYear() + '-' + e_month + '-' + e_date;
    document.getElementById("endDate").value = dateis;

    $(".getResult").click();
});
//open pending verification report
function openNewTabVerify() {
       var start = $(".startDate").val();
       var end = $(".endDate").val();
       var st_date = new Date(start);
       var et_date = new Date(end);

       var StartDateString = st_date.getFullYear() + "-" + (st_date.getMonth() + 1) + "-" + st_date.getDate();
       var endDateString = et_date.getFullYear() + "-" + (et_date.getMonth() + 1) + "-" + et_date.getDate();
       var url = "/openVerificationReport?startDate=" + StartDateString + "&endDate=" + endDateString ;
       window.open(url, '_blank');
   }
