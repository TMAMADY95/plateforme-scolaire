package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ExamenEtablissementService;
import com.plateforme.scolaire.web.dto.AffectationCentreRequest;
import com.plateforme.scolaire.web.dto.CentreExamenDto;
import com.plateforme.scolaire.web.dto.CentreExamenRequest;
import com.plateforme.scolaire.web.dto.ExamenEtablissementDto;
import com.plateforme.scolaire.web.dto.InscriptionCandidatsRequest;
import com.plateforme.scolaire.web.dto.PreparationExamenDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Examens côté établissement : candidats et convocations ; centres côté ministère et directions régionales. */
@RestController
@RequiredArgsConstructor
public class ExamenEtablissementController {

    private static final String SUPERVISION = "hasAnyRole('MINISTERE','DIRECTION_REGIONALE')";

    private final ExamenEtablissementService service;
    private final com.plateforme.scolaire.service.EpreuvesExamenService epreuves;

    @GetMapping("/api/etablissements/{id}/examens")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#id)")
    public List<ExamenEtablissementDto> examens(@PathVariable Long id, @RequestParam Long anneeScolaireId) {
        return service.examensDe(id, anneeScolaireId);
    }

    @GetMapping("/api/etablissements/{id}/examens/{examenId}/candidats")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#id)")
    public PreparationExamenDto preparation(@PathVariable Long id, @PathVariable Long examenId) {
        return service.preparation(id, examenId);
    }

    @PostMapping("/api/etablissements/{id}/examens/{examenId}/candidats")
    @PreAuthorize("@perimetre.peutGererEtablissement(#id)")
    public PreparationExamenDto inscrire(@PathVariable Long id, @PathVariable Long examenId,
                                         @Valid @RequestBody InscriptionCandidatsRequest r) {
        return service.inscrire(id, examenId, r.eleveIds());
    }

    @DeleteMapping("/api/candidats-examen/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void retirer(@PathVariable Long id) {
        service.retirer(id);
    }

    @GetMapping("/api/etablissements/{id}/examens/{examenId}/convocations.pdf")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#id)")
    public ResponseEntity<byte[]> convocations(@PathVariable Long id, @PathVariable Long examenId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"convocations.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.convocations(id, examenId));
    }

    @GetMapping("/api/etablissements/{id}/examens/{examenId}/releves.pdf")
    @PreAuthorize("@perimetre.peutAccederEtablissement(#id)")
    public ResponseEntity<byte[]> releves(@PathVariable Long id, @PathVariable Long examenId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"releves-de-notes.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.releves(id, examenId));
    }

    @GetMapping("/api/examens/{examenId}/centres")
    @PreAuthorize(SUPERVISION)
    public List<CentreExamenDto> centres(@PathVariable Long examenId) {
        return service.centres(examenId);
    }

    @PostMapping("/api/examens/{examenId}/centres")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(SUPERVISION)
    public CentreExamenDto creerCentre(@PathVariable Long examenId, @Valid @RequestBody CentreExamenRequest r) {
        return service.creerCentre(examenId, r);
    }

    @DeleteMapping("/api/centres-examen/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(SUPERVISION)
    public void supprimerCentre(@PathVariable Long id) {
        service.supprimerCentre(id);
    }

    @PutMapping("/api/examens/{examenId}/etablissements/{etablissementId}/centre")
    @PreAuthorize(SUPERVISION + " and @perimetre.peutAccederEtablissement(#etablissementId)")
    public int affecterCentre(@PathVariable Long examenId, @PathVariable Long etablissementId,
                              @Valid @RequestBody AffectationCentreRequest r) {
        return service.affecterCentre(examenId, etablissementId, r.centreId());
    }

    /** Emploi du temps officiel : lisible par les directions et la supervision. */
    @GetMapping("/api/examens/{examenId}/epreuves")
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE','DIRECTION_ETABLISSEMENT','PERSONNEL_ADMIN')")
    public List<com.plateforme.scolaire.web.dto.EpreuveDto> epreuves(@PathVariable Long examenId) {
        return epreuves.lister(examenId);
    }

    @PostMapping("/api/examens/{examenId}/epreuves")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(SUPERVISION)
    public com.plateforme.scolaire.web.dto.EpreuveDto ajouterEpreuve(@PathVariable Long examenId,
            @Valid @RequestBody com.plateforme.scolaire.web.dto.EpreuveRequest r) {
        return epreuves.ajouter(examenId, r);
    }

    @DeleteMapping("/api/epreuves-examen/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(SUPERVISION)
    public void supprimerEpreuve(@PathVariable Long id) {
        epreuves.supprimer(id);
    }

    /** Plan du centre : salles de 30 candidats au plus, tables et numéros de PV. */
    @GetMapping("/api/examens/{examenId}/centres/{centreId}/repartition")
    @PreAuthorize(SUPERVISION)
    public com.plateforme.scolaire.web.dto.RepartitionCentreDto repartition(@PathVariable Long examenId, @PathVariable Long centreId) {
        return service.repartition(examenId, centreId);
    }

    @GetMapping("/api/examens/{examenId}/centres/{centreId}/liste-appel.pdf")
    @PreAuthorize(SUPERVISION)
    public ResponseEntity<byte[]> listeAppel(@PathVariable Long examenId, @PathVariable Long centreId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"liste-appel.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.listeAppel(examenId, centreId));
    }

    @PostMapping("/api/examens/{examenId}/centres/{centreId}/repartition")
    @PreAuthorize(SUPERVISION)
    public com.plateforme.scolaire.web.dto.RepartitionCentreDto repartir(@PathVariable Long examenId, @PathVariable Long centreId) {
        return service.repartir(examenId, centreId);
    }

    /** Place dans un centre de leur commune les candidats qui n'en ont pas encore. */
    @PostMapping("/api/examens/{examenId}/affectation-automatique")
    @PreAuthorize(SUPERVISION)
    public com.plateforme.scolaire.web.dto.AffectationAutomatiqueDto affectationAutomatique(@PathVariable Long examenId) {
        return service.affecterAutomatiquement(examenId);
    }
}
