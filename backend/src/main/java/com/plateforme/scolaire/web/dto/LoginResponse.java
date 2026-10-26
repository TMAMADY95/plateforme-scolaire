package com.plateforme.scolaire.web.dto;

/** changementMotDePasseRequis = true : seules /api/me et /api/me/mot-de-passe sont accessibles. */
public record LoginResponse(String token, String type, Long utilisateurId, String email,
                            boolean changementMotDePasseRequis) {
}
