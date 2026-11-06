package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.TransfertEleve;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.TransfertEleveRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.TransfertEleveDto;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Transfert en cours d'année. L'école d'accueil le demande (par matricule), l'école d'origine l'approuve ou le refuse.
 * À l'approbation, l'inscription d'origine passe à TRANSFEREE et une inscription EN_ATTENTE est créée dans l'école d'accueil,
 * qui l'accepte ensuite avec une classe comme n'importe quelle inscription.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TransfertEleveService {

    static final Set<String> MOTIFS = Set.of("DEMENAGEMENT", "RAPPROCHEMENT_FAMILIAL", "ORIENTATION", "DISCIPLINE", "AUTRE");

    private final TransfertEleveRepository transferts;
    private final InscriptionRepository inscriptions;
    private final EleveRepository eleves;
    private final EtablissementRepository etablissements;
    private final AnneeScolaireRepository annees;
    private final PerimetreService perimetre;
    private final EntityManager em;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : seule une demande ouverte peut être décidée ou annulée. */
    static boolean ouverte(String statut) {
        return "DEMANDE".equals(statut);
    }

    /** Pur : une décision est APPROUVE ou REFUSE ; un refus doit être motivé. */
    static String verifierDecision(String decision, String motifRefus) {
        if (!"APPROUVE".equals(decision) && !"REFUSE".equals(decision)) {
            return "Décision inconnue : " + decision;
        }
        if ("REFUSE".equals(decision) && (motifRefus == null || motifRefus.isBlank())) {
            return "Le motif du refus est obligatoire";
        }
        return null;
    }

    private static ResponseStatusException erreur(HttpStatus s, String m) {
        return new ResponseStatusException(s, m);
    }

    private TransfertEleveDto.Transfert dto(TransfertEleve t) {
        Eleve e = eleves.findById(t.getEleveId()).orElseThrow();
        String origine = etablissements.findById(t.getEtablissementOrigineId()).map(Etablissement::getNom).orElse("?");
        String dest = etablissements.findById(t.getEtablissementDestId()).map(Etablissement::getNom).orElse("?");
        return new TransfertEleveDto.Transfert(t.getId(), e.getId(), e.getMatricule(), e.getPrenom() + " " + e.getNom(), t.getEtablissementOrigineId(), origine,
                t.getEtablissementDestId(), dest, t.getMotif(), t.getCommentaire(), t.getStatut(), t.getMotifRefus(), t.getDemandeLe(), t.getDemandePar(),
                t.getTraiteLe(), t.getTraitePar(), t.getNouvelleInscriptionId());
    }

    @Transactional(readOnly = true)
    public TransfertEleveDto lister(Long etablissementId, Long anneeId) {
        if (!etablissements.existsById(etablissementId)) {
            throw erreur(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        return new TransfertEleveDto(etablissementId, anneeId,
                transferts.findByEtablissementOrigineIdAndAnneeScolaireIdOrderByDemandeLeDescIdDesc(etablissementId, anneeId).stream().map(this::dto).toList(),
                transferts.findByEtablissementDestIdAndAnneeScolaireIdOrderByDemandeLeDescIdDesc(etablissementId, anneeId).stream().map(this::dto).toList());
    }

    public TransfertEleveDto.Transfert demander(Long destinationId, TransfertEleveDto.Demande q, String auteur) {
        Etablissement dest = etablissements.findById(destinationId).orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        if (!"ACTIF".equals(dest.getStatut())) {
            throw erreur(HttpStatus.CONFLICT, "L'établissement n'est pas actif");
        }
        AnneeScolaire annee = annees.findById(q.anneeId()).orElseThrow(() -> erreur(HttpStatus.BAD_REQUEST, "Année scolaire inconnue"));
        if (!MOTIFS.contains(q.motif())) {
            throw erreur(HttpStatus.BAD_REQUEST, "Motif inconnu : " + q.motif());
        }
        Eleve eleve = IdentifiantNational.trouver(eleves, q.matriculeEleve()).orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Aucun élève avec ce matricule"));
        Inscription origine = inscriptions.findByEleveIdAndAnneeScolaireIdAndStatutIn(eleve.getId(), annee.getId(), List.of("ACCEPTEE", "EN_ATTENTE"))
                .orElseThrow(() -> erreur(HttpStatus.CONFLICT, "Cet élève n'a aucune inscription active cette année : utilisez une inscription normale"));
        if (!"ACCEPTEE".equals(origine.getStatut())) {
            throw erreur(HttpStatus.CONFLICT, "L'inscription de cet élève n'est pas encore acceptée dans son établissement actuel");
        }
        if (origine.getEtablissement().getId().equals(destinationId)) {
            throw erreur(HttpStatus.CONFLICT, "Cet élève est déjà inscrit dans votre établissement");
        }
        if (transferts.existsByEleveIdAndAnneeScolaireIdAndStatut(eleve.getId(), annee.getId(), "DEMANDE")) {
            throw erreur(HttpStatus.CONFLICT, "Une demande de transfert est déjà en cours pour cet élève");
        }
        TransfertEleve t = new TransfertEleve();
        t.setEleveId(eleve.getId());
        t.setAnneeScolaireId(annee.getId());
        t.setEtablissementOrigineId(origine.getEtablissement().getId());
        t.setEtablissementDestId(destinationId);
        t.setMotif(q.motif());
        t.setCommentaire(q.commentaire() == null || q.commentaire().isBlank() ? null : q.commentaire().trim());
        t.setDemandeLe(LocalDate.now(clock));
        t.setDemandePar(auteur);
        transferts.save(t);
        audit.enregistrer("TRANSFERT_ELEVE_DEMANDE", "TransfertEleve", t.getId(), Map.of("eleve", eleve.getId(), "origine", t.getEtablissementOrigineId(), "destination", destinationId));
        return dto(t);
    }

    @SuppressWarnings("unchecked")
    private boolean estCandidat(Long eleveId, Long anneeId) {
        Number n = (Number) em.createNativeQuery("SELECT count(*) FROM candidat_examen c JOIN examen_national x ON x.id = c.examen_id "
                + "WHERE c.eleve_id = :e AND x.annee_scolaire_id = :a").setParameter("e", eleveId).setParameter("a", anneeId).getSingleResult();
        return n.longValue() > 0;
    }

    public TransfertEleveDto.Transfert decider(Long id, TransfertEleveDto.Decision q, String auteur) {
        TransfertEleve t = transferts.findById(id).orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Transfert introuvable"));
        if (!perimetre.peutGererEtablissement(t.getEtablissementOrigineId())) {
            throw erreur(HttpStatus.FORBIDDEN, "Seule la direction de l'établissement d'origine peut décider");
        }
        if (!ouverte(t.getStatut())) {
            throw erreur(HttpStatus.CONFLICT, "Cette demande a déjà été traitée");
        }
        String message = verifierDecision(q.decision(), q.motifRefus());
        if (message != null) {
            throw erreur(HttpStatus.BAD_REQUEST, message);
        }
        t.setTraiteLe(LocalDate.now(clock));
        t.setTraitePar(auteur);
        if ("REFUSE".equals(q.decision())) {
            t.setStatut("REFUSE");
            t.setMotifRefus(q.motifRefus().trim());
            audit.enregistrer("TRANSFERT_ELEVE_REFUSE", "TransfertEleve", id, Map.of("eleve", t.getEleveId()));
            return dto(t);
        }
        if (estCandidat(t.getEleveId(), t.getAnneeScolaireId())) {
            throw erreur(HttpStatus.CONFLICT, "Cet élève est candidat à un examen cette année : le retirer des candidats avant de le transférer");
        }
        Inscription origine = inscriptions.findByEleveIdAndAnneeScolaireIdAndStatutIn(t.getEleveId(), t.getAnneeScolaireId(), List.of("ACCEPTEE"))
                .orElseThrow(() -> erreur(HttpStatus.CONFLICT, "L'inscription d'origine n'est plus active"));
        if (!origine.getEtablissement().getId().equals(t.getEtablissementOrigineId())) {
            throw erreur(HttpStatus.CONFLICT, "L'élève n'est plus inscrit dans l'établissement d'origine");
        }
        origine.setStatut("TRANSFEREE");
        origine.setDateDecision(LocalDate.now(clock));
        inscriptions.saveAndFlush(origine);
        Inscription nouvelle = new Inscription();
        nouvelle.setEleve(origine.getEleve());
        nouvelle.setEtablissement(etablissements.getReferenceById(t.getEtablissementDestId()));
        nouvelle.setAnneeScolaire(origine.getAnneeScolaire());
        nouvelle.setStatut("EN_ATTENTE");
        nouvelle.setDateDemande(LocalDate.now(clock));
        inscriptions.saveAndFlush(nouvelle);
        t.setStatut("APPROUVE");
        t.setNouvelleInscriptionId(nouvelle.getId());
        audit.enregistrer("TRANSFERT_ELEVE_APPROUVE", "TransfertEleve", id, Map.of("eleve", t.getEleveId(), "nouvelleInscription", nouvelle.getId()));
        return dto(t);
    }

    public TransfertEleveDto.Transfert annuler(Long id) {
        TransfertEleve t = transferts.findById(id).orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Transfert introuvable"));
        if (!perimetre.peutGererEtablissement(t.getEtablissementDestId())) {
            throw erreur(HttpStatus.FORBIDDEN, "Seule la direction de l'établissement d'accueil peut annuler sa demande");
        }
        if (!ouverte(t.getStatut())) {
            throw erreur(HttpStatus.CONFLICT, "Cette demande a déjà été traitée");
        }
        t.setStatut("ANNULE");
        audit.enregistrer("TRANSFERT_ELEVE_ANNULE", "TransfertEleve", id, Map.of("eleve", t.getEleveId()));
        return dto(t);
    }
}
