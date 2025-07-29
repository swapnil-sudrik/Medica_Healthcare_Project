package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.models.HospitalizationInfo;
import com.fspl.medica_healthcare.repositories.HospitalizationInfoRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HospitalizationService {

    @Autowired
    private HospitalizationInfoRepository hospitalizationInfoRepository;


    private static final Logger log = Logger.getLogger(HospitalizationService.class);

    public HospitalizationInfo saveHospitalization(HospitalizationInfo hospitalizationInfo) {
        try {
            // Appointment appointment = hospitalizationInfo.getAppointment();
            HospitalizationInfo saved = hospitalizationInfoRepository.save(hospitalizationInfo);
            // appointment.setHospitalizationInfo(saved);
            // appointmentRepository.save(appointment);
            if (saved == null) {
                return null;
            }
            return saved;
        } catch (Exception e) {
            log.error("An error occurred while saving hospitalization information: "+ e.getMessage(), e);
            e.printStackTrace();
            return null;
        }
    }


    public HospitalizationInfo getHospitalizationInfoById(long id){
        try{
            return hospitalizationInfoRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.error("An error occurred while getting hospitalization information: "+ e.getMessage(), e);
            e.printStackTrace();
            return null;
        }
    }


    public boolean saveAllHospitalization(List<HospitalizationInfo> hospitalizationInfos){
        try {
            List<HospitalizationInfo> savedHospitalization = hospitalizationInfoRepository.saveAll(hospitalizationInfos);
            if (savedHospitalization.isEmpty()){
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An error occurred while saving hospitalization information: "+ e);
            return false;
        }
    }

    public List<HospitalizationInfo> findByDateOfDischargeIsNull(){
        try {
            List<HospitalizationInfo> hospitalizationInfos = hospitalizationInfoRepository.findByDateOfDischargeIsNull();
            if (hospitalizationInfos.isEmpty()){
                return null;
            }
            return hospitalizationInfos;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An error occurred while findByDateOfDischargeIsNull(): "+ e);
            return null;
        }
    }




}
