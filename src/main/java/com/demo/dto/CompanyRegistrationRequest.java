package com.demo.dto;

import lombok.Data;

@Data
public class CompanyRegistrationRequest {

    private String companyName;
    private String companyEmail;
    private String companyPhone;
    private String address;
    private String gstNumber;

    private String adminName;
    private String adminEmail;
    private String adminPhone;
    private String password;
}