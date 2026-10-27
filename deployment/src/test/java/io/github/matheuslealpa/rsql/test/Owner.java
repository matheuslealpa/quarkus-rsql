package io.github.matheuslealpa.rsql.test;

import jakarta.persistence.Entity;

import io.github.matheuslealpa.rsql.RsqlIgnore;

@Entity
public class Owner extends BaseEntity {

    public String name;

    @RsqlIgnore
    public String passwordHash;

}
