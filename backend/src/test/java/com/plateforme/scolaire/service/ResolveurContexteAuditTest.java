package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Evaluation;
import com.plateforme.scolaire.domain.Facture;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.JournalAudit;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Paiement;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.EvaluationRepository;
import com.plateforme.scolaire.repository.FactureRepository;
import com.plateforme.scolaire.repository.FraisRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.PaiementRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.repository.ZoneGeographiqueRepository;
import com.plateforme.scolaire.web.dto.ElementContexte;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResolveurContexteAuditTest {

    private final InscriptionRepository inscriptionRepository = mock(InscriptionRepository.class);
    private final EvaluationRepository evaluationRepository = mock(EvaluationRepository.class);
    private final PaiementRepository paiementRepository = mock(PaiementRepository.class);
    private final FactureRepository factureRepository = mock(FactureRepository.class);
    private final ClasseRepository classeRepository = mock(ClasseRepository.class);

    private final ResolveurContexteAudit resolveur = new ResolveurContexteAudit(inscriptionRepository,
            evaluationRepository, paiementRepository, factureRepository, mock(FraisRepository.class),
            mock(EtablissementRepository.class), classeRepository, mock(UtilisateurRepository.class),
            mock(ZoneGeographiqueRepository.class));

    private static JournalAudit entree(String action, Long entiteId, Map<String, Object> details) {
        JournalAudit j = new JournalAudit();
        j.setAction(action);
        j.setEntite("x");
        j.setEntiteId(entiteId);
        j.setDetails(details);
        return j;
    }

    private static Map<String, Object> details(Object... paires) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < paires.length; i += 2) {
            m.put((String) paires[i], paires[i + 1]);
        }
        return m;
    }

    private static Etablissement etablissement() {
        Etablissement e = new Etablissement();
        e.setId(7L);
        e.setNom("École du Centre");
        e.setCodeOfficiel("ECO-1");
        return e;
    }

    private static Eleve eleve() {
        Eleve e = new Eleve();
        e.setId(11L);
        e.setPrenom("Awa");
        e.setNom("Diop");
        e.setMatricule("E-000123");
        return e;
    }

    private static String valeur(java.util.List<ElementContexte> contexte, String libelle) {
        return contexte.stream().filter(c -> c.libelle().equals(libelle)).map(ElementContexte::valeur)
                .findFirst().orElse(null);
    }

    @Test
    void uneNoteModifieeIndiqueLEleveLEtablissementLaClasseEtLEvaluation() {
        Classe classe = new Classe();
        classe.setNom("6e A");
        Inscription inscription = new Inscription();
        inscription.setEleve(eleve());
        inscription.setEtablissement(etablissement());
        inscription.setClasse(classe);
        when(inscriptionRepository.findById(1L)).thenReturn(Optional.of(inscription));
        Matiere maths = new Matiere();
        maths.setLibelle("Mathématiques");
        Evaluation evaluation = new Evaluation();
        evaluation.setTitre("Devoir 1");
        evaluation.setMatiere(maths);
        when(evaluationRepository.findById(2L)).thenReturn(Optional.of(evaluation));

        var description = resolveur.decrire(entree("MODIFICATION_NOTE", 2L,
                details("evaluationId", 2, "inscriptionId", 1, "avant", "14.00", "apres", "16.00")));

        assertThat(description.resume()).isEqualTo("Note modifiée : 14,00 → 16,00");
        assertThat(valeur(description.contexte(), "Élève")).isEqualTo("Awa Diop (E-000123)");
        assertThat(valeur(description.contexte(), "Établissement")).isEqualTo("École du Centre (ECO-1)");
        assertThat(valeur(description.contexte(), "Classe")).isEqualTo("6e A");
        assertThat(valeur(description.contexte(), "Évaluation")).isEqualTo("Devoir 1 (Mathématiques)");
    }

    @Test
    void unObjetSupprimeDepuisEstOmisSansErreur() {
        when(inscriptionRepository.findById(1L)).thenReturn(Optional.empty());
        when(evaluationRepository.findById(2L)).thenReturn(Optional.empty());

        var description = resolveur.decrire(entree("SUPPRESSION_NOTE", 2L,
                details("evaluationId", 2, "inscriptionId", 1, "avant", "ABSENT")));

        assertThat(description.resume()).isEqualTo("Note supprimée (elle valait absent)");
        assertThat(description.contexte()).isEmpty();
    }

    @Test
    void unEncaissementIndiqueLeLEleveEtLEtablissementDeLaFacture() {
        Facture facture = new Facture();
        facture.setEleve(eleve());
        facture.setEtablissement(etablissement());
        facture.setNumero("F-2026-0001");
        facture.setLibelle("Scolarité");
        Paiement paiement = new Paiement();
        paiement.setFacture(facture);
        when(paiementRepository.findById(5L)).thenReturn(Optional.of(paiement));

        var description = resolveur.decrire(entree("ENCAISSEMENT", 5L,
                details("facture", "F-2026-0001", "montant", "25000.00", "mode", "ESPECES")));

        assertThat(description.resume())
                .isEqualTo("Encaissement de 25000,00 (espèces) sur la facture F-2026-0001");
        assertThat(valeur(description.contexte(), "Élève")).isEqualTo("Awa Diop (E-000123)");
        assertThat(valeur(description.contexte(), "Facture")).isEqualTo("F-2026-0001 — Scolarité");
    }

    @Test
    void lesActionsDInscriptionEtDAppelOntUnephraseLisible() {
        assertThat(resolveur.decrire(entree("DEMANDE_INSCRIPTION", 1L, details("nouvelEleve", true))).resume())
                .isEqualTo("Inscription demandée pour un nouvel élève");
        assertThat(resolveur.decrire(entree("ACCEPTATION_INSCRIPTION", 1L, details("classeId", 3))).resume())
                .isEqualTo("Inscription acceptée");
        assertThat(resolveur.decrire(entree("ANNULATION_INSCRIPTION", 1L, details("statutAvant", "ACCEPTEE"))).resume())
                .isEqualTo("Inscription annulée (elle était acceptée)");
        assertThat(resolveur.decrire(entree("SAISIE_APPEL", 3L,
                details("classeId", 3, "date", "2026-10-26", "presents", 25, "absents", 2, "retards", 1,
                        "creees", 28, "corrigees", 0))).resume())
                .isEqualTo("Appel du 26/10/2026 : 25 présent(s), 2 absent(s), 1 retard(s)");
        assertThat(resolveur.decrire(entree("CORRECTION_PRESENCE", 40L,
                details("inscriptionId", 1, "date", "2026-10-26", "avant", "ABSENT", "apres", "PRESENT"))).resume())
                .isEqualTo("Présence du 26/10/2026 corrigée : absent → présent");
    }

    @Test
    void laGenerationSansClasseNeCherchePasDeClasse() {
        when(inscriptionRepository.findById(anyLong())).thenReturn(Optional.empty());

        var description = resolveur.decrire(entree("GENERATION_FACTURES", null,
                details("etablissementId", 7, "classeId", "aucune", "creees", 30, "ignorees", 2)));

        assertThat(description.resume()).isEqualTo("30 facture(s) générée(s), 2 déjà existante(s)");
        verify(classeRepository, never()).findById(anyLong());
    }

    @Test
    void uneActionInconnueRetombeSurUnLibelleLisible() {
        var description = resolveur.decrire(entree("NOUVELLE_ACTION_FUTURE", null, null));

        assertThat(description.resume()).isEqualTo("Nouvelle action future");
        assertThat(description.contexte()).isEmpty();
    }
}
