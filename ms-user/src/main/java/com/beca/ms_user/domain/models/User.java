package com.beca.ms_user.domain.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;
import java.util.UUID;

public class User {

    private UUID id;
    private String name;
    private String email;
    private String cpf;
    private String phone;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;

    public User() {

    }

    public User(UUID id, String name, String email, String cpf, String phone, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.cpf = cpf;
        this.phone = phone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static User prepareCreate(String name, String email, String cpf, String phone) {
        return new User(null, name, email, cpf, phone, null, null, null);
    }

    public void updatePersonalData(String newName, String newPhone) {
        this.name = newName;
        this.phone = newPhone;
        this.updatedAt = OffsetDateTime.now();
    }

    public static User prepareUpdate(UUID id, String name, String phone) {
        return new User(id, name, null, null, phone, null, null, null);
    }

    public void delete() {
        this.deletedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getCpf() {
        return cpf;
    }

    public String getPhone() {
        return phone;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

}
