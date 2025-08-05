$(".go_btn").attr('disabled', true);
$(".reqMsg").hide();

function handleAcknowledgedList() {
    let parcelId = $("#parcelval").val().trim();
    if (!parcelId){
        return;
    }
    let tests = "";
    let barcodeStatus = false;
    $.ajax({
        type: "GET",
        url: "/findByTrackingId",
        data: "parcelId=" + parcelId,
        dataType: "html",
        success: function(data) {
            let listAre = JSON.parse(data);
            console.log(listAre);
            let count = listAre.length;

            if ($(".boyName").val() == '' || $(".boyNo").val() == null) {
                $(".reqMsg").show();
                $("#parcelval").val('');
                $(".boyName").val('');
                $(".boyNo").val('');
            } else {
                $(".reqMsg").hide();
                $(".data-table").empty();

                if (count == 0) {
                    $(".repeatMsg").text("Sample Already Acknowledged");
                    $(".date").hide();
                }

                for (let i = 0; i < count; i++) {
                    let repeat = listAre[i].orderDetail.sampleStatus;
                    if (repeat != "" && repeat == "PICKED") {
                        $(".data-table").append(
                            '<tr class="col-md-12 parentdiv" id="row' + i + '" >' +
                            "<td class=''>" +
                            '<span>' + listAre[i].order.labName + '</span>' +
                            "</td>" +
                            "<td class=''>" +
                            '<input value="' + listAre[i].samplePickupId + '" type="hidden" class="samplePickupId" /><input type="hidden" class="flagIs" id="rec' + i + '" /><input class="form-control barcodeAre" id="barcode' + i + '" type="text" value="' + listAre[i].orderDetail.barcode + '" readonly />' +
                            "</td>" +
                            "<td>" +
                            '<span>' + listAre[i].order.patient.title + ' ' + listAre[i].order.patient.firstName + ' ' + listAre[i].order.patient.middleName + ' ' + listAre[i].order.patient.lastName + ' ' + listAre[i].order.patient.relationWithPatient + ' ' + listAre[i].order.patient.relativeName + ' ' + listAre[i].order.patient.age + '</span>' +
                            "</td>" +
                            "<td>" +
                            '<span>' + listAre[i].testNames + '</span>' +
                            "</td>" +
                            "</td>" +
                            "<td>" +
                            ' <select onchange="statusChange(' + i + ');" id="status' + i + '" class="form-control statusIs"><option value="">--Select--</option><option value="Received & Accepted">Received & Accepted</option><option value="Not Received">Not Received</option></select>' +
                            "</td>" +
                            "<td>" +
                            ' <textarea class="mark" id="mark' + i + '"></textarea><div class="c_msg text-danger" id="comment' + i + '">Required</div>' +
                            "</td>" +
                            /*+ "<td class='col-md-2'>"
                            + '<input type="checkbox" class="chk" />'
                            + "</td>"*/
                            + '</tr>');
                        $("#comment" + i).hide();
                        $(".repeatMsg").text(" ");
                        $(".date").show();
                    } else {
                        //do nothing
                        $(".repeatMsg").text("Sample Already Acknowledged");
                        $(".date").hide();
                    }
                }
            }
        }
    });
}

$("#parcelval").on('blur', function(event) {
    event.preventDefault();
    handleAcknowledgedList();
});

$("#parcelval").on("keypress", function(event) {
    if (event.keyCode === 13) {
        event.preventDefault();
        handleAcknowledgedList();
    }
});




$("#sample").on("change", function() {
	let len = $(".barcodeAre").length;
	$(".go_btn").attr('disabled', false);
	for (let k=0; k <= len; k++) {
		if ($("#sample").val() == $(".barcodeAre").eq(k).val()) {
			//$('.chk:checkbox').eq(k).prop('checked', true);
			$(".barcodeAre").eq(k).css("background-color", "green");
			$(".barcodeAre").eq(k).css("color", "white");
			$("#sample").val("");
			$(".flagIs").eq(k).val(1);
			$(".mark").eq(k).val(" ");
			$(".statusIs").eq(k).val("Received & Accepted");
			$('.barcodeAre').eq(k).attr('name', 'barcode');
			$('.samplePickupId').eq(k).attr('name', 'pick_id');
			$('.flagIs').eq(k).attr('name', 'flag');
			$('.statusIs').eq(k).attr('name', 's_status');
			$('.mark').eq(k).attr('name', 'remark');
		}
	}

});

function statusChange(i) {
	if($("#status"+i).val() == "Not Received") {
		$("#barcode"+i).css("statusstatusstatusbackground-color", "");
		$("#barcode"+i).css("color", "");
		$("#comment"+i).prop("required", true);
		$("#comment"+i).show();
		$("#rec"+i).val(0);
	} else {
		$("#barcode"+i).css("background-color", "green");
		$("#barcode"+i).css("color", "white");
		$("#comment"+i).hide();
		$("#rec"+i).val(1);
		$("#mark"+i).val(" ");
	}

};

$(".go_btn").on("click", function() {

	 $('#updateForm').submit();
});
