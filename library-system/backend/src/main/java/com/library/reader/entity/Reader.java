package com.library.reader.entity;

import com.library.common.persistence.SoftDeletableEntity;
import com.library.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "readers")
public class Reader extends SoftDeletableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "reader_code", nullable = false, unique = true, length = 50)
    private String readerCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reader_type_id", nullable = false)
    private ReaderType readerType;

    private LocalDate dob;

    @Column(length = 20)
    private String gender;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "id_number", length = 50)
    private String idNumber;

    @Column(name = "student_code", length = 50)
    private String studentCode;

    @Column(length = 255)
    private String organization;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private boolean blacklisted = false;

    @Column(name = "blacklist_reason", columnDefinition = "TEXT")
    private String blacklistReason;

    @Column(name = "no_show_count", nullable = false)
    private int noShowCount = 0;

    @Column(name = "registered_at")
    private Instant registeredAt;
}
