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
@Table(name = "authors")
public class Author extends SoftDeletableEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "pen_name")
    private String penName;

    @Column(length = 100)
    private String nationality;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "death_year")
    private Integer deathYear;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;
}
