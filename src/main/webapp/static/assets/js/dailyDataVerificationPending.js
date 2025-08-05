$(document).ready(function() {
         $('.age').each(function() {
                var dob = $(this).val();
                var birthdate = new Date(dob);
                var today = new Date();
                var ageToday = today.getFullYear() - birthdate.getFullYear();
                var m = today.getMonth() - birthdate.getMonth();
                if (m < 0 || (m === 0 && today.getDate() < birthdate.getDate())) {
                    ageToday--;
                }
                $(this).closest('td').find('.ageToday').text(ageToday); // Set ageToday as text of the span within the same <td>
            });

    var table = $('#multicolumn_ordering_table').DataTable({
        columnDefs: [
            { targets: [0], orderable: false }
        ],
        dom: 'lrtip'
    });

    // Verify all/none functionality
    $('#verify').on('click', function() {
        $('.verifySelected').prop('checked', this.checked);
        toggleVerifyButton();
    });

    $('.verifySelected').on('click', function() {
        if ($('.verifySelected:checked').length == $('.verifySelected').length) {
            $('#verify').prop('checked', true);
        } else {
            $('#verify').prop('checked', false);
        }
        toggleVerifyButton();
    });

    function toggleVerifyButton() {
        if ($('.verifySelected:checked').length > 0) {
            $("#verifyButton").show();
        } else {
            $("#verifyButton").hide();
        }
    }
    $('#hubFilter').on('change', function () {
        var selectedHub = $(this).val();
        if (selectedHub) {
            table.columns(9).search('^' + selectedHub + '$', true, false).draw();
        } else {
            table.columns(9).search('').draw();
        }
    });
});