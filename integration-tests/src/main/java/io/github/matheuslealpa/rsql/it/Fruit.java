package io.github.matheuslealpa.rsql.it;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

import io.github.matheuslealpa.rsql.RsqlIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Fruit extends PanacheEntity {

    public String name;

    public int weight;

    @Enumerated(EnumType.STRING)
    public Color color;

    public LocalDate harvested;

    public Boolean organic;

    @ManyToOne
    public Farm farm;

    @Transient
    public String label;

    @RsqlIgnore
    public String supplierCode;

    public enum Color {
        RED,
        GREEN,
        YELLOW
    }

}
