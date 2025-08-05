$(".searchableSelectbox").select2({ width: '150px' });

// Hide elements initially
$(".msg").hide();
$(".msgbar").hide();
$(".pno").hide();
$(".go_btn").attr('disabled', true);
$(".repeat").hide();

// Event handler for barcode input change
$('#barcode').on("change", function() {
    let barcode = $('#barcode').val().trim();
    let boxNo = $('#box').val().trim();
    let spoke = $(".spokeName").val().trim();
    let collectedBy = $(".collectedBy").val().trim();
    let mobile = $(".mobile").val().trim();
    let barcodeStatus = false;

    if (!barcode) {
        // Early return if barcode is empty
        return;
    }

    // AJAX request to get details by barcode
    $.ajax({
        type: "GET",
        url: "/findDetailsBybarcode",
        data: { sampleid: barcode },
        dataType: "html",
        success: function(data) {
            let details = JSON.parse(data);
            if (details.length === 0) {
                $(".repeat").show().text("Sample doesn't exist");
                $('#barcode').val('');
                return;
            }

            for (let detail of details) {
                let sampleStatus = detail.orderDetail.sampleStatus;
                if (sampleStatus === 'PICKED') {
                    barcodeStatus = true;
                    $(".repeat").show().text("Sample Already Collected");
                    $('#barcode').val('');
                    return;
                } else if (['Received & Accepted', 'Received & Rejected', 'Received But QNS', 'Not Received'].includes(sampleStatus)) {
                    barcodeStatus = true;
                    $(".repeat").show().text("Sample Already Acknowledged");
                    $('#barcode').val('');
                    return;
                } else {
                    $(".repeat").hide();
                }
            }

            // Validate mandatory fields
            if (!boxNo || !spoke || !collectedBy || !mobile) {
                $('#box').attr('readonly', false);
                $(".msg").show();
                $('#box, .spokeName, .collectedBy, .mobile, #barcode').val('');
                return;
            }

            // Check for duplicate barcode
            let duplicate = $(".barcodeAre").filter(function() {
                return $(this).val() === barcode;
            }).length > 0;

            if (duplicate) {
                barcodeStatus = true;
                $(".msgbar").show();
                $('#barcode').val('');
                return;
            }

            $(".go_btn").attr('disabled', false);
            $('#box').attr('readonly', true);
            $(".msg").hide();

            let allTestName = details.map(detail => detail.orderDetail.testName).join("<br />");

            if (!barcodeStatus) {
                $(".spokeis").val(spoke);
                $(".parcel").val($(".pno").text());
                $(".msgbar").hide();
                $(".data-table").append(
                    `<tr class='col-md-12 parentdiv'>
                        <td><span>${spoke}</span></td>
                        <td><span>${boxNo}</span></td>
                        <td><input class="form-control barcodeAre" type="text" value="${barcode}" name="barcode" readonly /></td>
                        <td><span>${details[0].order.patient.title} ${details[0].order.patient.firstName} ${details[0].order.patient.middleName} ${details[0].order.patient.lastName} ${details[0].order.patient.relationWithPatient} ${details[0].order.patient.relativeName} ${details[0].order.patient.age}</span></td>
                        <td><span>${allTestName}</span></td>
                        <td class='col-md-2'><a class="btn removeBtn"><svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" class="bi bi-backspace-reverse-fill" viewBox="0 0 16 16"><path d="M0 3a2 2 0 0 1 2-2h7.08a2 2 0 0 1 1.519.698l4.843 5.651a1 1 0 0 1 0 1.302L10.6 14.3a2 2 0 0 1-1.52.7H2a2 2 0 0 1-2-2V3zm9.854 2.854a.5.5 0 0 0-.708-.708L7 7.293 4.854 5.146a.5.5 0 1 0-.708.708L6.293 8l-2.147 2.146a.5.5 0 0 0 .708.708L7 8.707l2.146 2.147a.5.5 0 0 0 .708-.708L7.707 8l2.147-2.146z"/></svg></a></td>
                    </tr>`
                );
                $('#barcode').val('');
            }
        },
        error: function() {
            $(".repeat").show().text("An error occurred while fetching data.");
        }
    });
});

// Event handler to remove a row
$("body").on("click", ".removeBtn", function() {
    $(this).closest(".parentdiv").remove();
    if ($(".barcodeAre").length < 1) {
        $(".go_btn").attr('disabled', true);
    }
});

// Event handler to submit the form
$(".go_btn").on("click", function() {
    $('#submitForm').submit();
});