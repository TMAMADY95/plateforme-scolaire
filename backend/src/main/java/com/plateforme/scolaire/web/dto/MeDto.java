package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.security.UtilisateurPrincipal;
import java.util.List;

public record MeDto(Long id, String email, boolean changementMotDePasseRequis,
                    List<UtilisateurPrincipal.AffectationInfo> affectations) {
}
