package com.fspl.medica_healthcare.services;
import com.fspl.medica_healthcare.controllers.SettingsController;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.Settings;
import com.fspl.medica_healthcare.repositories.HospitalRepository;
import com.fspl.medica_healthcare.repositories.SettingsRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SettingsService {

    private static final Logger logger = Logger.getLogger(SettingsService.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private HospitalRepository hospitalRepository;


    // Find existing setting by using provided hospitalId
    public Optional<Hospital> findHospitalByHospitalId(Long hospitalId) {

        try {
            return hospitalRepository.findById(hospitalId);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error in fetching hospital with given hospital ID  : {} "+sw);
            return Optional.empty();
        }
    }

    public Optional<Settings> findSettingsByHospitalId(Long hospitalId) {

        try {
            return settingsRepository.findByHospital_Id(hospitalId);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error in fetching hospital with given hospital ID  : {} "+sw);
            return Optional.empty();
        }
    }

    //Find setting by hospital
    public Optional<Settings> findSettingsByHospital(Hospital hospital) {

        try {
            return settingsRepository.findByHospital(hospital);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error in fetching settings for hospital: {}"+sw);
            return Optional.empty();
        }
    }

    //Save Settings
    public void saveSettings(Settings settings, MultipartFile hospitalLetterHead, MultipartFile hospitalLogo) throws IOException {

        try {
            if (hospitalLetterHead != null && !hospitalLetterHead.isEmpty()) {
                settings.setHospitalLetterHead(hospitalLetterHead.getBytes()); // Save letterhead bytes
            }

            if (hospitalLogo != null && !hospitalLogo.isEmpty()) {
                settings.setHospitalLogo(hospitalLogo.getBytes()); // Save logo bytes
            }

            // Save or update the settings entity
            settingsRepository.save(settings);

        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error Occurred in Save Settings :{}"+sw);

        }
    }

    // Get All Settings

    public List<Settings> getAllSettings() {

        try {
            return settingsRepository.findAll();
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error fetching all setting : {} "+sw);
            return new ArrayList<>();
        }
    }


    // Get letterHead
    public Optional<byte[]> getLetterHead(long settingId) {

        try {
            return settingsRepository.findLetterHead(settingId);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error fetching letterhead for given hospital ID : {} " +sw);
            return Optional.empty();
        }
    }

    // Get Hospital Logo
    public Optional<byte[]> getLogo(long settingId) {

        try {
            return settingsRepository.findLetterLogo(settingId);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error fetching logo for hospital id : {}" +sw);
            return Optional.empty();
        }
    }

    //Get Settings by settingId
    public Settings getSettingsById(long settingId) {

        try {
            return settingsRepository.findById(settingId).orElse(null);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error in fetching settings for given Setting ID : {} " +sw);
            return null;
        }
    }
}