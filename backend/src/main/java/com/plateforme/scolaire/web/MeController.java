package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.service.SecuriteComptesService;
import com.plateforme.scolaire.web.dto.ChangementMotDePasseRequest;
import com.plateforme.scolaire.web.dto.MeDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final PerimetreService perimetreService;
    private final SecuriteComptesService securiteComptesService;

    @GetMapping
    public MeDto moi() {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        return new MeDto(p.getId(), p.getUsername(), p.isDoitChangerMotDePasse(), p.getAffectations());
    }

    /** Après le changement, tous les anciens jetons sont révoqués : il faut se reconnecter. */
    @PostMapping("/mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changerMotDePasse(@Valid @RequestBody ChangementMotDePasseRequest requete) {
        securiteComptesService.changerMotDePasse(perimetreService.utilisateurCourant().getId(), requete);
    }
}
