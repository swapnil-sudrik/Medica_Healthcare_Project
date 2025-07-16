//package com.fspl.medica_healthcare.mapper;
//
//import com.fspl.medica_healthcare.dtos.HospitalResponseDTO;
//import com.fspl.medica_healthcare.dtos.UserResponseDTO;
//import com.fspl.medica_healthcare.models.Hospital;
//import com.fspl.medica_healthcare.models.User;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//public class UserMapper {
//
//    public static UserResponseDTO toUserResponseDTO(User user) {
//        UserResponseDTO dto = new UserResponseDTO();
//        dto.setUserId(user.getUserId());
//        dto.setUsername(user.getUsername());
//        dto.setRoles(user.getRoles());
//        dto.setName(user.getName());
//
//        // Include Hospital details in the DTO response
//        if (user.getHospitalId() != null) {
//            dto.setHospital(toHospitalResponseDTO(user.getHospitalId()));
//        }
//
//        return dto;
//    }
//
//    public static List<UserResponseDTO> mapUsersToResponseDTOList(List<User> users) {
//        return users.stream()
//                    .map(user -> {
//                        UserResponseDTO dto = new UserResponseDTO();
//                        dto.setUserId(user.getUserId());
//                        dto.setUsername(user.getUsername());
//                        dto.setRoles(user.getRoles());
//                        dto.setName(user.getName());
//
//                        // Include Hospital details in the DTO response
//                        if (user.getHospitalId() != null) {
//                            dto.setHospital(toHospitalResponseDTO(user.getHospitalId()));
//                        }
//
//                        return dto;
//                    })
//                    .collect(Collectors.toList());
//    }
//
//
//    public static HospitalResponseDTO toHospitalResponseDTO(Hospital hospital) {
//        HospitalResponseDTO hospitalDto = new HospitalResponseDTO();
//        hospitalDto.setHospitalId(hospital.getHospitalId());
//        hospitalDto.setHospitalName(hospital.getHospitalName());
//        hospitalDto.setHospitalAddress(hospital.getHospitalAddress());
//        hospitalDto.setHospitalContactNumber(hospital.getHospitalContactNumber());
//        hospitalDto.setHospitalEmailId(hospital.getHospitalEmailId());
//        return hospitalDto;
//    }
//
//}
//
