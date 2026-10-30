package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ReferentielService;
import com.plateforme.scolaire.web.dto.AnneeScolaireDto;
import com.plateforme.scolaire.web.dto.AnneeScolaireRequest;
import com.plateforme.scolaire.web.dto.MatiereDto;
import com.plateforme.scolaire.web.dto.MatiereRequest;
import com.plateforme.scolaire.web.dto.NiveauDto;
import com.plateforme.scolaire.web.dto.NiveauRequest;
import com.plateforme.scolaire.web.dto.PeriodeDto;
import com.plateforme.scolaire.web.dto.PeriodeRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lecture : tout utilisateur connecté. Écriture : ministère ou administrateur technique.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReferentielController {

    private static final String GESTION_REFERENTIEL = "hasAnyRole('MINISTERE', 'ADMIN_TECHNIQUE')";

    private final ReferentielService referentielService;

    // ---------- Années scolaires et périodes ----------

    @GetMapping("/annees-scolaires")
    public List<AnneeScolaireDto> annees() {
        return referentielService.annees();
    }

    @PostMapping("/annees-scolaires")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(GESTION_REFERENTIEL)
    public AnneeScolaireDto creerAnnee(@Valid @RequestBody AnneeScolaireRequest requete) {
        return referentielService.creerAnnee(requete);
    }

    @PutMapping("/annees-scolaires/{id}")
    @PreAuthorize(GESTION_REFERENTIEL)
    public AnneeScolaireDto modifierAnnee(@PathVariable Long id, @Valid @RequestBody AnneeScolaireRequest requete) {
        return referentielService.modifierAnnee(id, requete);
    }

    @GetMapping("/annees-scolaires/{id}/periodes")
    public List<PeriodeDto> periodes(@PathVariable Long id) {
        return referentielService.periodes(id);
    }

    @PostMapping("/annees-scolaires/{id}/periodes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(GESTION_REFERENTIEL)
    public PeriodeDto creerPeriode(@PathVariable Long id, @Valid @RequestBody PeriodeRequest requete) {
        return referentielService.creerPeriode(id, requete);
    }

    @PutMapping("/periodes/{id}")
    @PreAuthorize(GESTION_REFERENTIEL)
    public PeriodeDto modifierPeriode(@PathVariable Long id, @Valid @RequestBody PeriodeRequest requete) {
        return referentielService.modifierPeriode(id, requete);
    }

    // ---------- Niveaux ----------

    @GetMapping("/niveaux")
    public List<NiveauDto> niveaux() {
        return referentielService.niveaux();
    }

    @PostMapping("/niveaux")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(GESTION_REFERENTIEL)
    public NiveauDto creerNiveau(@Valid @RequestBody NiveauRequest requete) {
        return referentielService.creerNiveau(requete);
    }

    @PutMapping("/niveaux/{id}")
    @PreAuthorize(GESTION_REFERENTIEL)
    public NiveauDto modifierNiveau(@PathVariable Long id, @Valid @RequestBody NiveauRequest requete) {
        return referentielService.modifierNiveau(id, requete);
    }

    // ---------- Matières ----------

    @GetMapping("/matieres")
    public List<MatiereDto> matieres() {
        return referentielService.matieres();
    }

    @PostMapping("/matieres")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(GESTION_REFERENTIEL)
    public MatiereDto creerMatiere(@Valid @RequestBody MatiereRequest requete) {
        return referentielService.creerMatiere(requete);
    }

    @PutMapping("/matieres/{id}")
    @PreAuthorize(GESTION_REFERENTIEL)
    public MatiereDto modifierMatiere(@PathVariable Long id, @Valid @RequestBody MatiereRequest requete) {
        return referentielService.modifierMatiere(id, requete);
    }
}
