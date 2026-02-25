package com.kpbridge.kpbridge.dto;

public class MemberRequestDto {

    // 1. 데이터를 담을 그릇 (필드)
    private String userId;
    private String password;
    private String userName;
    private String phone;
    private String email;
    private String referralCode;
    private String birthDate;

    // 2. 기본 생성자
    public MemberRequestDto() {}

    // ★★★ 3. 수동 Getter / Setter (이게 있어야 빨간 줄이 사라집니다!) ★★★
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
}