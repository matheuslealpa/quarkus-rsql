package io.github.matheuslealpa.rsql.test;

import java.util.List;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Farm extends BaseEntity {

    public String name;

    public String country;

    @Embedded
    public Address address;

    @ManyToOne
    public Owner owner;

    @OneToMany(mappedBy = "farm")
    public List<Fruit> fruits;

    @ElementCollection
    public Map<String, String> attributes;

}
