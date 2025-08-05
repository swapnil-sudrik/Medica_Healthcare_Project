  $(document).ready(function () {
        var today = new Date();
        today.setDate(today.getDate() + 10); // Add 10 days to today's date
        var maxDate = new Date(today);

        $('#lab_mean').pickadate({
            weekdaysShort: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
            labelMonthNext: 'Go to the next month',
            labelMonthPrev: 'Go to the previous month',
            labelMonthSelect: 'Pick a month from the dropdown',
            labelYearSelect: 'Pick a year from the dropdown',
            formatSubmit: 'yyyy/mm/dd',
            format: 'yyyy/mm/dd',
            min: maxDate,
            selectYears: 100,
            selectMonths: true
        });

        $('#controlForm').submit(function (event) {

            function areFieldsEmpty(fields) {
                for (var i = 0; i < fields.length; i++) {
                    if ($(fields[i]).val().trim() === '') {
                        return fields[i];
                    }
                }
                return null;
            }

            event.preventDefault();

            var fieldsToCheck = ['#machineName', '#parameterName', '#lotNo', '#level', '#mean', '#sd'];
            var emptyField = areFieldsEmpty(fieldsToCheck);
            if (emptyField) {
                $(emptyField).css('border-color', '#F5A9A9');
                $(emptyField).siblings('span.clientmessage').addClass('error');
                $(emptyField).siblings('span.clientmessage').text('This field is mandatory');
                $(emptyField).on("change", function () {
                    $(emptyField).css('border-color', '#CCCCCC');
                    $(emptyField).siblings('span.clientmessage').removeClass('error');
                    $(emptyField).siblings('span.clientmessage').text('');
                });
            } else {

                var formData = $(this).serialize();

                // Submit form data asynchronously using AJAX
                $.ajax({
                    type: 'POST',
                    url: $(this).attr('action'),
                    data: formData,
                    success: function (response) {
                        // Parse JSON response
                        var jsonResponse = JSON.parse(response);

                        if (jsonResponse.success || jsonResponse.edited) {
                            // Determine the appropriate message based on whether it's an edit or add operation
                            var successMessage = jsonResponse.success ? 'Control Mapped Successfully' : 'Mapping Updated Successfully';

                            // Show success message using SweetAlert
                            swal({
                                type: 'success',
                                icon: 'success',
                                title: successMessage,
                                showConfirmButton: false,
                                timer: 1500
                            })
                            setTimeout(function () {
                                window.location.href = '/quality/controlValueMappingList';
                            }, 1500);
                        }
                    },
                    error: function () {
                        // Show error message using SweetAlert if AJAX request fails
                        swal({
                            icon: 'error',
                            title: 'Error',
                            text: 'An error occurred while processing your request'
                        });
                    }
                });
            }
        });
        function updateLevels() {
    var lotNo = $('#lotNo').val();
    if (lotNo) {
        $.ajax({
            url: '/quality/getLevelsByLotNo',
            type: 'GET',
            data: { lotNo: lotNo },
            success: function(data) {
                $('#level').empty();
                $('#level').append('<option value="">Select</option>');
                for (var i = 1; i <= data; i++) {
                    $('#level').append('<option value="Level ' + i + '">Level ' + i + '</option>');
                }
                // Set the selected value if editing
                var currentLevel = "[[${range.level}]]";
                if (currentLevel) {
                    $('#level').val(currentLevel);
                }
            }
        });
    } else {
        $('#level').empty();
        $('#level').append('<option value="">Select</option>');
    }
}

        updateLevels();
        $('#lotNo').change(updateLevels);
    });
