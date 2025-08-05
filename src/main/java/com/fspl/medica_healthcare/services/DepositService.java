package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.models.Deposit;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.DepositRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepositService {

    @Autowired
    private DepositRepository depositRepository;

    // reason of taking loginUser is for set log which loginUser add that deposit
    public boolean addDeposit(User loginUser, Deposit deposit){
        try{
            depositRepository.save(deposit);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public List<Deposit> getAllDepositByAppointmentId(User loginUser,long appointmentId){
        try{
            List<Deposit> allDepositByAppointmentId = depositRepository.findAllDepositByAppointmentId(appointmentId);
            return allDepositByAppointmentId;
//            return allDepositByAppointmentId != null || !allDepositByAppointmentId.isEmpty() ? allDepositByAppointmentId : null;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
