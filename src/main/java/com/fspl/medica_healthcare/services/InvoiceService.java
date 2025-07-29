package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.enums.InvoiceStatus;
import com.fspl.medica_healthcare.models.Invoice;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.AppointmentRepository;
import com.fspl.medica_healthcare.repositories.InvoiceRepository;
import com.fspl.medica_healthcare.templets.EmailTemplets;
import com.fspl.medica_healthcare.templets.PdfTemplate;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository billingRepository;

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

    //    private static final Logger log = LogManager.getLogger(InvoiceService.class);
    private static final Logger log = Logger.getLogger(InvoiceService.class);
    // =======================================================================================================================
    // UPDATED METHOD OPTIMIZED CODE AND PROPER HANDLE EXCEPTIONS
    // =======================================================================================================================
    public boolean saveInvoice(User loginUser, Invoice bill) {
        try {
            billingRepository.save(bill);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Saving Invoice: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return false;
        }
    }

    public List<Invoice> getInvoiceByStatus(long hospitalId,byte[] branch, InvoiceStatus status) {
        try {
//            return billingRepository.findByHospitalIdAndStatusIn(loginUser.getHospital().getId(), status);
            return billingRepository.findInvoiceByStatus(hospitalId,branch, status);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Status: \n"+ ExceptionUtils.getStackTrace(e) );
            return null;
        }
    }


    public List<Invoice> getInvoiceByBalanceRange(long hospitalId,byte[] branch, double minBalance, double maxBalance) {
        try {
//            return billingRepository.findByBalanceAmountRange(loginUser.getHospital().getId(), minBalance, maxBalance);
            return billingRepository.findInvoiceByBalanceRange(hospitalId,branch, minBalance, maxBalance);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Balance Range: \n"+ ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    public Invoice getInvoiceById(long hospitalId,byte[] branch,long id) {
        try {
//            Optional<Invoice> bill = billingRepository.findByAppointment_Hospital_IdAndId(loginUser.getHospital().getId(),id);
            Optional<Invoice> bill = billingRepository.findInvoiceById(hospitalId,branch,id);
            return bill.orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Id: \n"+ ExceptionUtils.getStackTrace(e) );
            return null;
        }
    }

    public Invoice getInvoiceById(long id){
        try {
            Optional<Invoice> bill = billingRepository.findById(id);
            return bill.orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Id: \n"+ ExceptionUtils.getStackTrace(e) +"\n");
            return null;
        }
    }


    public List<Invoice> getAllInvoice(long hospitalId,byte[] branch) {
        try {
//            return billingRepository.findAll();
            return billingRepository.findAllInvoiceByHospitalId(hospitalId,branch);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice: \n"+ ExceptionUtils.getStackTrace(e) );
            return null;
        }
    }

    public List<Invoice> getPendingInvoice(User loginUser, BigDecimal balanceAmount) {
        try {
//            List<Invoice> bills = billingRepository.findInvoiceWithBalance(loginUser.getHospital().getId(), balanceAmount);
            List<Invoice> bills = billingRepository.findPendingInvoice(loginUser.getHospital().getId(), balanceAmount);
            return (bills != null) ? (!bills.isEmpty()) ? bills : null : null;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Pending Invoice : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return null;
        }
    }

    public Invoice getInvoiceByAppointmentId(long hospitalId,byte[] branch,long appointmentId) {
        try {
//            Optional<Invoice> byAppointmentAppointmentId = billingRepository.findByAppointment_Id(appointmentId);
//            Optional<Invoice> byAppointmentAppointmentId = billingRepository.findByAppointment_Hospital_IdAndAppointment_Id(loginUser.getHospital().getId(),appointmentId);
            Optional<Invoice> byAppointmentId = billingRepository.findInvoiceByAppointment_Id(hospitalId,branch,appointmentId);
            return byAppointmentId.orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Appointment Id: \n"+ ExceptionUtils.getStackTrace(e) );
            return null;
        }
    }

    public Invoice getInvoiceByPatientId(long hospitalId,byte[] branch,long id) {
        try {
            return billingRepository.findInvoiceByPatientId(hospitalId,branch,id);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Patient Id : \n"+ ExceptionUtils.getStackTrace(e) );
            return null;
        }
    }

    public List<Invoice> getInvoiceByPatientName(long hospitalId,byte[] branch,byte[] name) {
        try {
//            return billingRepository.findByAppointment_Patient_Name(loginUser.getHospital().getId(),name);
            return billingRepository.findInvoiceByPatientName(hospitalId,branch,name);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Patient Name : \n"+ ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    public List<Invoice> getInvoiceByMonthAndYear(long hospitalId,byte[] branch, int month, int year) {
        try {
//            return billingRepository.findInvoiceByDate(loginUser.getHospital().getId(), month, year);
            return billingRepository.findInvoiceByMonthAndYear(hospitalId,branch, month, year);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Invoice By Date Range : \n"+ ExceptionUtils.getStackTrace(e));
            return null;
        }
    }


    public LocalDate getEarliestInvoiceDate(long hospitalId,byte[] branch) {
        try {
            return billingRepository.findEarliestInvoiceDate(hospitalId,branch);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Date Of Earliest Invoice : \n"+ ExceptionUtils.getStackTrace(e));
            return null;
        }
    }


    public List<Invoice> getInvoiceByDateRange(long hospitalId,byte[] branch, LocalDate startDate, LocalDate endDate) {
        try {
//            return billingRepository.findByCreatedDateBetween(loginUser.getHospital().getId(), startDate, endDate);
            return billingRepository.findInvoiceByDateRange(hospitalId,branch, startDate, endDate);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Invoice By Date Range : \n"+ ExceptionUtils.getStackTrace(e));
            return null;
        }
    }


    public Double getTotalRevenueByHospitalId(long hospitalId,byte[] branch) {
        try {
//            return billingRepository.getTotalRevenueByHospitalId(loginUser.getHospital().getId());
            return billingRepository.findTotalRevenueByHospitalId(hospitalId,branch);
        } catch (Exception e) {
            log.error("Failed to fetch revenue by hospital!! :: 'error' : "+ e.getMessage());
            return null;
        }
    }

    public Double getTotalRevenueByMonthAndYear(long hospitalId,byte[] branch, int month, int year) {
        try {
//            return billingRepository.getTotalRevenueByHospitalIdAndDate(loginUser.getHospital().getId(), month, year);
            return billingRepository.findTotalRevenueByMonthAndYear(hospitalId,branch, month, year);
        } catch (Exception e) {
            log.error("Failed to fetch revenue by hospital date wise!! :: 'error' : "+ e.getMessage());
            return null;
        }
    }

    public List<Invoice> getDueInvoice() {
        try {
//            return billingRepository.findInvoiceWithDueDates();
            return billingRepository.findInvoiceByDueDate();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Due Invoice : \n"+ ExceptionUtils.getStackTrace(e) +"\n");
            return null;
        }
    }

    public List<Invoice> getAllInvoiceByHospitalId(long hospitalId,byte[] branch) {
        try {
//            List<Invoice> billingList = billingRepository.findAllHospitalInvoiceByHospitalId(loginUser.getHospital().getId());
            List<Invoice> billingList = billingRepository.findAllInvoiceByHospitalId(hospitalId,branch);
            if (billingList.isEmpty()) {
                return null;
            }
            return billingList;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Invoice By Hospital Id : \n"+ ExceptionUtils.getStackTrace(e));
            return null;
        }
    }


    public List<Invoice> getInvoiceOfCompleteStatus() {
        try {
//            return billingRepository.findByHospitalIdAndStatusIn(loginUser.getHospital().getId(), status);
//            return billingRepository.findInvoiceByStatus(loginUser.getHospital().getId(), status);
            return billingRepository.findInvoiceOfCompleteStatus(InvoiceStatus.COMPLETE);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Status: \n"+ ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

}





//
//public boolean existsByAppointmentId(User loginUser,long id) {
//    try {
////            return billingRepository.existsByAppointment_Id(id);
//        return billingRepository.existsByAppointment_Hospital_IdAndAppointment_Id(loginUser.getHospital().getId(),id);
//    } catch (Exception e) {
//        e.printStackTrace();
//        log.error("An unexpected error occurred while Checking Bill Exists or Not: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
//        return false;
//    }
//}


//public Invoice getByAppointmentId(User loginUser,long appointmentId) {
//    try {
////            return billingRepository.findByAppointment_Id(appointmentId).orElse(null);
//        return billingRepository.findByAppointment_Hospital_IdAndAppointment_Id(loginUser.getHospital().getId(),appointmentId).orElse(null);
//    } catch (Exception e) {
//        e.printStackTrace();
//        log.error("An unexpected error occurred while Fetch Bill By Appointment Id : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
//        return null;
//    }
//}


//    public Billing getBillById(User loginUser, long id) {
//        try {
//            Optional<Billing> bill;
//
//            if (loginUser != null) {
//                bill = billingRepository.findByAppointment_Hospital_IdAndId(loginUser.getHospital().getId(), id);
//            } else {
//                bill = billingRepository.findById(id);
//            }
//
//            return bill.orElse(null);
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error("An unexpected error occurred while fetching Bill By Id: \n"
//                    + ExceptionUtils.getStackTrace(e)
//                    + (loginUser != null ? "\nLogged User: " + loginUser.getId() : ""));
//            return null;
//        }
//    }


//public List<Billing> getDueBills(User loginUser) {
//    try {
//        return billingRepository.findBillsWithDueDates();
//    } catch (Exception e) {
//        e.printStackTrace();
//        log.error("An unexpected error occurred while Fetch Due Bills : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
//        return null;
//    }
//}