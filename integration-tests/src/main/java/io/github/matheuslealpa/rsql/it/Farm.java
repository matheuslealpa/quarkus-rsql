package io.github.matheuslealpa.rsql.it;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Farm extends PanacheEntity {

    public String name;

    public String country;

    @OneToMany(mappedBy = "farm")
    public List<Fruit> fruits;

}
