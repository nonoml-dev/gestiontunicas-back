package es.nonomldev.gestiontunicas.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Refresh token para renovar la sesión o cerrarla. */
public record RefreshTokenRequest(@NotBlank @Size(max = 100) String refreshToken) {
}
