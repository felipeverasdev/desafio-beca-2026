package com.beca.ms_user.domain.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public class User {

    private UUID id;
    private String name;
    private String email;
    private String password;
    private String cpf;
    private String phone;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;

    public User() {

    }

    public User(UUID id, String name, String email, String password, String cpf, String phone, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.cpf = cpf;
        this.phone = phone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static User create(String name, String email, String password, String cpf, String phone) {
        return new User(
                null,
                name,
                email,
                password,
                cpf,
                phone,
                null,
                null,
                null
        );
    }

    public void updatePersonalData(String newName, String newEmail, String newPhone) {
        this.name = newName;
        this.email = newEmail;
        this.phone = newPhone;
        this.updatedAt = OffsetDateTime.now();
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

    public String getPassword() {
        return password;
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
