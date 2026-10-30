package com.backend.bangbangbwa.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(length = 50)
    private String nickname;

    @Column(length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UserRole role;

    private LocalDate birth;

    @Column(length = 20)
    private String phone;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private User(
            String nickname,
            String name,
            LocalDate birth,
            String phone
    ) {
        this.nickname = nickname;
        this.name = name;
        this.birth = birth;
        this.phone = phone;
        this.role = UserRole.TENANT;
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.role == null) {
            this.role = UserRole.TENANT;
        }
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(
            String nickname,
            LocalDate birth,
            String phone
    ) {
        if (StringUtils.hasText(nickname)) {
            this.nickname = nickname;
        }

        if (birth != null) {
            this.birth = birth;
        }

        if (StringUtils.hasText(phone)) {
            this.phone = phone;
        }
    }

    public void promoteToAgent() {
        this.role = UserRole.AGENT;
    }

    public boolean isAgent() {
        return this.role == UserRole.AGENT;
    }

    public boolean isTenant() {
        return this.role == UserRole.TENANT;
    }

    public void delete() {
        this.nickname = null;
        this.name = null;
        this.birth = null;
        this.phone = null;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}