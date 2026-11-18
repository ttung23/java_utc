package com.library.catalog.entity;

import com.library.common.persistence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "locations")
public class Location extends SoftDeletableEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 100)
    private String zone;

    @Column(length = 50)
    private String floor;

    @Column(name = "row", length = 50)
    private String row;

    @Column(length = 50)
    private String shelf;

    @Column(length = 50)
    private String slot;

    private Integer capacity;

    @Column(columnDefinition = "TEXT")
    private String note;
}
