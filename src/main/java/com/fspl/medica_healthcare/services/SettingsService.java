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


    public Optional<Hospital> findByHospital(Long id) {

        try {
            return hospitalRepository.findById(id);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error in fetching hospital with given hospital ID  : {} "+sw);
            return Optional.empty();
        }
    }

    public Optional<Settings> findSettingsByHospital(Hospital hospital) {

        try {
            return settingsRepository.findByHospital(hospital);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error in fetching settings for hospital: {}"+sw);
            return Optional.empty();
        }
    }

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


    public List<Settings> getAllSettings() {

        try {
            return settingsRepository.findAll();
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error fetching all setting : {} "+sw);
            return new ArrayList<>();
        }
    }

    public Optional<byte[]> getLetterHead(long id) {

        try {
            return settingsRepository.findLetterHead(id);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error fetching letterhead for given hospital ID : {} " +sw);
            return Optional.empty();
        }
    }

    public Optional<byte[]> getLogo(long id) {

        try {
            return settingsRepository.findLetterLogo(id);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error fetching logo for hospital id : {}" +sw);
            return Optional.empty();
        }
    }

    public Settings getSettingsById(long id) {

        try {
            return settingsRepository.findById(id).orElse(null);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error in fetching settings for given Setting ID : {} " +sw);
            return null;
        }
    }


}