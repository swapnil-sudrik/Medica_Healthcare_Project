package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;


@Entity
@Data
//@JsonIdentityInfo(
//        generator = ObjectIdGenerators.PropertyGenerator.class,
//        property = "id",
//        scope = User.class
//)
//@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String username;

    private String name;

    private String password;

    @Lob
    private byte[] branch;

    @Column(nullable = false)
    private String roles;

    @ManyToOne
//    @JsonBackReference
    @JsonIgnoreProperties({"createdUser", "modifiedUser"}) // Ignore to prevent recursion
    private Hospital hospital;

    @ManyToOne
    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser", "staff"}) // Ignore to prevent recursion
    private User createdUser;

    @ManyToOne
//    @JsonBackReference
    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser", "staff"}) // Ignore to prevent recursion
    private User modifiedUser;

    private LocalDate createdDate;

    private LocalDate modifiedDate;

    private int status;

    ///arshad , saurabh start
    private LocalDate lastUpdatedPasswordDate;
    /// arshad, saurabh end

    /// vishesh start
    private String type;

    ///pratik start
    private String contactNumber;

    private LocalDate dateOfBirth;
    /// pratik end

    ///deepali start
    private int failedLoginAttempts = 3;
    private boolean isAccountLocked = false;
    private LocalDateTime lockTime;
    /// deepali end


    @OneToOne
    @JsonIgnoreProperties({"hospital", "createdUser", "modifiedUser"}) // Ignore to prevent recursion
    private Staff staff;



    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", roles='" + roles + '\'' +
//                ", hospital=" + hospital.getHospitalName() +
                ", createdDate=" + createdDate +
                ", modifiedDate=" + modifiedDate +
                ", status=" + status +
                ", branch=" + new String(branch) +

                '}';
    }



}

