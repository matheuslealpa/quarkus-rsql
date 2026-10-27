package io.github.matheuslealpa.rsql.test;

import jakarta.persistence.Entity;

import io.github.matheuslealpa.rsql.RsqlIgnore;

@Entity
public class Owner extends BaseEntity {

    public String name;

    @RsqlIgnore
    public String passwordHash;

    public String apiToken;

    public boolean verified;

    @RsqlIgnore
    public String getApiToken() {
        return this.apiToken;
    }

    @RsqlIgnore
    public boolean isVerified() {
        return this.verified;
    }

}
