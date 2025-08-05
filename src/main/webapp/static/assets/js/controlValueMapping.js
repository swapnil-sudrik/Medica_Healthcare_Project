$(document).ready(function() {
    $.ajax({
        url: "/qcm/controlValueMappingListSearch",
        method: "GET",
        success: function(data) {
            var tableBody = $('#tableBody');
            data.forEach(function(item, index) {
                var row = '<tr data-id="' + item.id + '">' +
                             '<td>' + (index + 1) + '</td>' +
                             '<td>' + new Date(item.createdAt).toLocaleDateString() + '</td>' +
                             '<td>' + item.machineName + '</td>' +
                             '<td>' + item.parameterName + '</td>' +
                             '<td>' + item.lotNo + '</td>' +
                             '<td>' + item.level + '</td>' +
                             '<td class="editable">' + item.qcMean + '</td>' +
                             '<td class="editable">' + item.qcSd + '</td>' +
                             '<td class="editable">' + item.labSdMeanDate + '</td>' +
                             '<td><button class="btn btn-primary edit-button">Edit</button></td>' +
                           '</tr>';
                tableBody.append(row);
            });

            $('#tableBody').on('click', '.edit-button', function() {
                var row = $(this).closest('tr');
                if ($(this).text() === 'Edit') {
                    // Make the Mean and SD cells editable
                    row.find('.editable').each(function(index) {
                        var text = $(this).text();
                        if (index < 2) {
                            $(this).html('<input type="text" class="form-control" value="' + text + '">');
                        } else {
                            $(this).html('<input type="date" class="form-control" value="' + text + '">');
                        }
                    });
                    $(this).text('Save');
                } else {
                    // Save the updated values
                    var mean = row.find('.editable').eq(0).find('input').val();
                    var sd = row.find('.editable').eq(1).find('input').val();
                    var sdMeanDate = row.find('.editable').eq(2).find('input').val();
                    row.find('.editable').eq(0).html(mean);
                    row.find('.editable').eq(1).html(sd);
                    row.find('.editable').eq(2).html(sdMeanDate);

                    // Get the ID and other necessary values
                    var id = row.data('id');
                    var machineName = row.find('td').eq(2).text();
                    var parameterName = row.find('td').eq(3).text();
                    var lotNo = row.find('td').eq(4).text();
                    var level = row.find('td').eq(5).text();
                    var dbContext = $('#dbContext').val();

                    // Create the object to be sent to the server
                    var rowData = {
                        id: id,
                        machineName: machineName,
                        parameterName: parameterName,
                        level: level,
                        lotNo: lotNo,
                        qcMean: mean,
                        qcSd: sd,
                        labSdMeanDate: sdMeanDate,
                        dbContext: dbContext
                    };
                    // Send the data to the server using AJAX
                    $.ajax({
                        url: '/ajax/controlValueMappingFormUpdate/' + id,
                        method: 'POST',
                        data: rowData,
                        success: function(response) {
                            if (response.status === "OK") {
                                swal({
                                    type: 'success',
                                    icon: 'success',
                                    title: 'Update Successful',
                                    text: response.message,
                                    showConfirmButton: false,
                                    timer: 1500
                                });
                            } else {
                                swal({
                                    type: 'error',
                                    icon: 'error',
                                    title: 'Update Failed',
                                    text: response.message || 'An unexpected error occurred.',
                                    showConfirmButton: true
                                });
                            }
                        },
                        error: function(xhr, status, error) {
                            swal({
                                type: 'error',
                                icon: 'error',
                                title: 'Update Failed',
                                text: 'An error occurred while processing your request.',
                                showConfirmButton: true
                            });
                        }
                    });

                    $(this).text('Edit');
                }
            });
        },
        error: function(error) {
            console.log("Error fetching data", error);
        }
    });
});
