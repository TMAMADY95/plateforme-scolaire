package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.IdentifiantNationalService;
import com.plateforme.scolaire.web.dto.EtatCivilRequest;
import com.plateforme.scolaire.web.dto.IdentiteNationaleDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Identifiant national de l'élève (INE) et état civil. Les contrôles d'accès reprennent ceux d'EleveController. */
@RestController
@RequestMapping("/api/eleves")
@RequiredArgsConstructor
public class IdentifiantNationalController {

    private final IdentifiantNationalService service;

    /**
     * Retrouver un élève par son INE (transfert, inscription dans un nouvel établissement).
     * L'INE n'est pas un secret et n'importe qui peut le saisir : le périmètre est donc vérifié APRÈS la
     * recherche (@PostAuthorize), et le service renvoie le même message pour un INE inconnu et hors périmètre.
     */
    @GetMapping("/ine/{ine}")
    @PostAuthorize("@perimetre.peutAccederEleve(returnObject.id())")
    public IdentiteNationaleDto parIne(@PathVariable String ine) {
        return service.parIne(ine);
    }

    @GetMapping("/{id}/identite-nationale")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public IdentiteNationaleDto identite(@PathVariable Long id) {
        return service.identite(id);
    }

    @PutMapping("/{id}/etat-civil")
    @PreAuthorize("@perimetre.peutGererEleve(#id)")
    public IdentiteNationaleDto modifierEtatCivil(@PathVariable Long id, @Valid @RequestBody EtatCivilRequest r) {
        return service.modifierEtatCivil(id, r);
    }

    /** Élèves rattachés à l'établissement et sans acte de naissance, à régulariser avant les examens. */
    @GetMapping("/sans-acte")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#etablissementId)")
    public List<IdentiteNationaleDto> sansActe(@RequestParam Long etablissementId) {
        return service.sansActe(etablissementId);
    }
}
