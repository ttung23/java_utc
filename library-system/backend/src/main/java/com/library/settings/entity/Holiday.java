package com.library.settings.entity;

import com.library.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "holidays", uniqueConstraints = @UniqueConstraint(columnNames = {"date", "name"}))
public class Holiday extends BaseEntity {

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_recurring", nullable = false)
    private boolean recurring = false;
}
