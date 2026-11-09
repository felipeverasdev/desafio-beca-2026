package com.beca.ms_user.infrastructure.adapters.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, message = "O nome deve conter pelo menos 3 caracteres")
        String name,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "O número de celular é obrigatório")
        @Pattern(regexp = "^[1-9]{2}[1-9]{2}9[0-9]{8}$",
                message = "Formato de número de celular inválido. Exemplo correto: 5511912345678")
        String phone
) {
}
