//package com.fspl.medica_healthcare.mapper;
//
//import com.fspl.medica_healthcare.dtos.DepartmentResponseDTO;
//import com.fspl.medica_healthcare.dtos.HospitalResponseDTO;
//import com.fspl.medica_healthcare.dtos.UserResponseDTO;
//import com.fspl.medica_healthcare.models.Hospital;
//import com.fspl.medica_healthcare.models.Department;
//import com.fspl.medica_healthcare.models.User;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//
//public class DepartmentMapper
//{
//    public static DepartmentResponseDTO toDepartmentResponseDTO(Department department) {
//        DepartmentResponseDTO dto = new DepartmentResponseDTO();
//        dto.setDepartmentId(department.getDepartmentId());
//        dto.setName(department.getName());
//
//        // Include Hospital details in the DTO response
//        if (department.getHospitalId() != null) {
//            dto.setHospital(toHospitalResponseDTO(department.getHospitalId()));
//        }
//
//        return dto;
//    }
//
//    public static List<DepartmentResponseDTO> mapDepartmentToResponseDTOList(List<Department> departments) {
//        return departments.stream()
//                .map(department -> {
//                    DepartmentResponseDTO dto = new DepartmentResponseDTO();
//                    dto.setDepartmentId(department.getDepartmentId());
//                    dto.setName(department.getName());
//
//                    // Include Hospital details in the DTO response
//                    if (department.getHospitalId() != null) {
//                        dto.setHospital(toHospitalResponseDTO(department.getHospitalId()));
//                    }
//                    return dto;
//                })
//                .collect(Collectors.toList());
//    }
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
