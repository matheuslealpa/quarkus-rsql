package io.github.matheuslealpa.rsql.it;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Farm extends PanacheEntity {

    public String name;

    public String country;

}
