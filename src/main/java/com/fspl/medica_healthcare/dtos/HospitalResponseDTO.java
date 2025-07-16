package com.fspl.medica_healthcare.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Setter;

@Data
@Setter
public class HospitalResponseDTO {
    private long id;

//    ^[A-Za-z ]+$
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Hospital Name must contain only alphabetic characters")
    @Size(max = 100, message = "Hospital Name cannot exceed 100 characters")
    @Column(nullable = false)
    @NotBlank(message = "Hospital name is mandatory")
    private String name;

    @Lob
    private String address;

    @NotBlank(message = "Hospital contact number is mandatory")
    @Pattern(regexp = "\\d{10}", message = "Contact number must be exactly 10 digits or please put the correct number")
    private String contactNumber;

    @NotBlank(message = "Hospital is mandatory")
    @Email(message = "Hospital must be a valid email address")
    @Size(max = 100, message = "Hospital cannot exceed 100 characters")
    @Column(nullable = false, unique = true)
    private String emailId;

    @NotBlank(message = "Hospital Opening time is mandatory")
    private String openingTime;

    @NotBlank(message = "Hospital Closing time is mandatory")
    private String closingTime;

    @Lob
    @JsonIgnore
    private byte[] letterHead;

    @Lob
    private String departments;

    @Lob
    private String branch;

    @Pattern(regexp = "^[^\\d]+$", message = "Hospital working day cannot contain numbers")
    @Size(max = 100, message = "Working days cannot exceed 100 characters")
    private String workingDays;


    @Pattern(regexp = "^[^\\d]+$", message = "Hospital working day cannot contain numbers")
    @Size(max = 100, message = "Off days cannot exceed 100 characters")
    private String offDays;

    @NotBlank(message = "Number of users is mandatory")
    private String numberOfUsers;

    private int status;

    public void setLetterhead(byte[] letterHead) {
        this.letterHead= letterHead;
    }


}
