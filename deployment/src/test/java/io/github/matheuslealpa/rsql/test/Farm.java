package io.github.matheuslealpa.rsql.test;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class Farm extends BaseEntity {

    public String name;

    public String country;

    @Embedded
    public Address address;

    @ManyToOne
    public Owner owner;

}
