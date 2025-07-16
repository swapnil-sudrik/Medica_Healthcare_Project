package com.fspl.medica_healthcare.dtos;

import lombok.Data;

@Data
public class LoginResponse {
	private Long id;
	private String roles;
	private String token;

}
