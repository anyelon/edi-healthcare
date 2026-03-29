package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "providers")
public class Provider {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String npi;
    private String taxonomyCode;
    private String practiceId;

    public Provider() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getNpi() { return npi; }
    public void setNpi(String npi) { this.npi = npi; }

    public String getTaxonomyCode() { return taxonomyCode; }
    public void setTaxonomyCode(String taxonomyCode) { this.taxonomyCode = taxonomyCode; }

    public String getPracticeId() { return practiceId; }
    public void setPracticeId(String practiceId) { this.practiceId = practiceId; }
}
