package com.technical.aemet.infrastructure.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "municipalities")
public class MunicipalityEntity {
    @Id
    private String code;
    private String name;
    private String province;

    protected MunicipalityEntity() {
    }

    public MunicipalityEntity(String code, String name, String province) {
        this.code = code;
        this.name = name;
        this.province = province;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getProvince() {
        return province;
    }
}
