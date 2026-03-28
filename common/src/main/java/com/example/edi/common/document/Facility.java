package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "facilities")
public class Facility {

    @Id
    private String id;
    private String name;
    private String practiceId;
    private String placeOfServiceCode;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phone;
    private String npi;

    public Facility() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPracticeId() { return practiceId; }
    public void setPracticeId(String practiceId) { this.practiceId = practiceId; }

    public String getPlaceOfServiceCode() { return placeOfServiceCode; }
    public void setPlaceOfServiceCode(String placeOfServiceCode) { this.placeOfServiceCode = placeOfServiceCode; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNpi() { return npi; }
    public void setNpi(String npi) { this.npi = npi; }
}
