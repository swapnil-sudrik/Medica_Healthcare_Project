function validateAadharNumber(input) {
    var aadharNumber = input.value.replace(/\D/g, ''); // Remove non-numeric characters
    input.value = aadharNumber; // Update the input value with the cleaned numeric value

    if (aadharNumber.length !== 12) {
        document.getElementById('aadhaarNumberError').innerText = 'Aadhar Number should be exactly 12 digits';
        input.setCustomValidity('Aadhar Number should be exactly 12 digits');
    } else {
        document.getElementById('aadhaarNumberError').innerText = '';
        input.setCustomValidity('');
    }
}


    function validateNumber(input) {
        var moNumber = input.value;

        // Check for non-numeric characters
        if (/[^0-9]/.test(moNumber)) {
            document.getElementById('moNumberError').innerText = 'Phone Number should contain only numeric digits';
            input.setCustomValidity('Phone Number should contain only numeric digits');
        } else {
            // Check for the exact length of 10 digits
            if (moNumber.length !== 10) {
                document.getElementById('moNumberError').innerText = 'Phone Number should be exactly 10 digits';
                input.setCustomValidity('Phone Number should be exactly 10 digits');
            } else {
                // Clear any previous error messages
                document.getElementById('moNumberError').innerText = '';
                input.setCustomValidity('');
            }
        }
    }

     function validateABHAAddress() {
       var abhaAddressInput = document.getElementById("abhaAddressInput");
             var abhaAddressError = document.getElementById("abhaAddressError");
             var inputValue = abhaAddressInput.value;
           // Remove non-alphanumeric characters
           inputValue = inputValue.replace(/[^a-zA-Z0-9]/g, '');

           // Truncate to a maximum length of 10 characters
           inputValue = inputValue.substring(0, 20);

           // Update the input value
           abhaAddressInput.value = inputValue;
       }

