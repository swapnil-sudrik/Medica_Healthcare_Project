package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.enums.BillingStatus;
import com.fspl.medica_healthcare.models.Billing;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.AppointmentRepository;
import com.fspl.medica_healthcare.repositories.BillingRepository;
import com.fspl.medica_healthcare.templets.EmailTemplets;
import com.fspl.medica_healthcare.templets.PdfTemplate;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BillingService {

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private PdfTemplate pdfTemplate;

    @Autowired
    private EmailTemplets emailTemplets;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private HospitalizationService hospitalizationService;

    @Autowired
    private UserService userService;

    private static final Logger log = LogManager.getLogger(BillingService.class);
    // =======================================================================================================================
    // UPDATED METHOD OPTIMIZED CODE AND PROPER HANDLE EXCEPTIONS
    // =======================================================================================================================
    public boolean saveBill(User loginUser, Billing bill) {
        try {
            billingRepository.save(bill);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Saving Bill: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return false;
        }
    }

    public List<Billing> getAllHospitalBillsByHospitalId(User loginUser) {
        try {
            List<Billing> billingList = billingRepository.findAllHospitalBillsByHospitalId(loginUser.getHospital().getId());
            if (billingList.isEmpty()) {
                return null;
            }
            return billingList;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Bills By Hospital Id : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }

    public List<Billing> getBillsByStatus(User loginUser, List<BillingStatus> status) {
        try {
            return billingRepository.findByHospitalIdAndStatusIn(loginUser.getHospital().getId(), status);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bill By Status: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }


    public List<Billing> getBillsByBalanceRange(User loginUser, BigDecimal minBalance, BigDecimal maxBalance) {
        try {
            return billingRepository.findByBalanceAmountRange(loginUser.getHospital().getId(), minBalance, maxBalance);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bills By Balance Range: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }

    public Billing getBillById(User loginUser,long id) {
        try {
            Optional<Billing> bill = billingRepository.findByAppointment_Hospital_IdAndId(loginUser.getHospital().getId(),id);
            return bill.orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bill By Id: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }

    public List<Billing> getAllBills(User loginUser) {
        try {
            return billingRepository.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bills: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }

    public List<Billing> getPendingBills(User loginUser, BigDecimal balanceAmount) {
        try {
            List<Billing> bills = billingRepository.findBillingsWithBalance(loginUser.getHospital().getId(), balanceAmount);
            return (bills != null) ? (!bills.isEmpty()) ? bills : null : null;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Pending Bills : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }

    public Billing getBillByAppointmentId(User loginUser,long appointmentId) {
        try {
//            Optional<Billing> byAppointmentAppointmentId = billingRepository.findByAppointment_Id(appointmentId);
            Optional<Billing> byAppointmentAppointmentId = billingRepository.findByAppointment_Hospital_IdAndAppointment_Id(loginUser.getHospital().getId(),appointmentId);
            return byAppointmentAppointmentId.orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bill By Appointment Id: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }


    public Double getTotalRevenueByHospitalId(User loginUser) {
        try {
            return billingRepository.getTotalRevenueByHospitalId(loginUser.getHospital().getId());
        } catch (Exception e) {
            log.error("Failed to fetch revenue by hospital!! :: 'error' : "+ e.getMessage());
            return null;
        }
    }

    public Double getTotalRevenueByHospitalIdAndDate(User loginUser, int month, int year) {
        try {
            return billingRepository.getTotalRevenueByHospitalIdAndDate(loginUser.getHospital().getId(), month, year);
        } catch (Exception e) {
            log.error("Failed to fetch revenue by hospital date wise!! :: 'error' : "+ e.getMessage());
            return null;
        }
    }

    public boolean existsByAppointmentId(User loginUser,long id) {
        try {
//            return billingRepository.existsByAppointment_Id(id);
            return billingRepository.existsByAppointment_Hospital_IdAndAppointment_Id(loginUser.getHospital().getId(),id);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Checking Bill Exists or Not: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return false;
        }
    }

    public List<Billing> getBillByPatientName(User loginUser,String name) {
        try {
            return billingRepository.findByAppointment_Patient_Name(loginUser.getHospital().getId(),name);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bill By Patient Name : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }

    public List<Billing> getBillsByDate(User loginUser, int month, int year) {
        try {
            return billingRepository.findBillsByDate(loginUser.getHospital().getId(), month, year);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Bills By Date Range : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }


    public LocalDate getEarliestBillingDate(User loginUser) {
        try {
            return billingRepository.findEarliestBillingDate();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Date Of Earliest Bill : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }


    public List<Billing> getBillByDateRange(User loginUser, LocalDate startDate, LocalDate endDate) {
        try {
            return billingRepository.findByCreatedDateBetween(loginUser.getHospital().getId(), startDate, endDate);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Bill By Date Range : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }

    public Billing getByAppointmentId(User loginUser,long appointmentId) {
        try {
//            return billingRepository.findByAppointment_Id(appointmentId).orElse(null);
            return billingRepository.findByAppointment_Hospital_IdAndAppointment_Id(loginUser.getHospital().getId(),appointmentId).orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bill By Appointment Id : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }


    public List<Billing> getDueBills(User loginUser) {
        try {
            return billingRepository.findBillsWithDueDates();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Due Bills : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }


    public Billing getBillsByPatientId(User loginUser,long id) {
        try {
            return billingRepository.findByPatientId(loginUser.getHospital().getId(),id);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bill By Patient Id : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }


}
