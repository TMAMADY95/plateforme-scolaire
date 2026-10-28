package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Evaluation;
import com.plateforme.scolaire.domain.Facture;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.JournalAudit;
import com.plateforme.scolaire.domain.Utilisateur;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Transforme une entrée brute du journal en phrase lisible et en éléments d'identification
 * (élève, établissement, classe, facture, compte). Les données sont relues au moment de l'affichage :
 * un objet supprimé depuis est simplement omis, jamais une erreur. À appeler dans une transaction.
 */
@Component
@RequiredArgsConstructor
public class ResolveurContexteAudit {

    public record Description(String resume, List<ElementContexte> contexte) {
    }

    private final InscriptionRepository inscriptionRepository;
    private final EvaluationRepository evaluationRepository;
    private final PaiementRepository paiementRepository;
    private final FactureRepository factureRepository;
    private final FraisRepository fraisRepository;
    private final EtablissementRepository etablissementRepository;
    private final ClasseRepository classeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ZoneGeographiqueRepository zoneRepository;

    public Description decrire(JournalAudit j) {
        Map<String, Object> d = j.getDetails();
        List<ElementContexte> contexte = new ArrayList<>();
        String action = j.getAction();
        Long entiteId = j.getEntiteId();

        String resume = switch (action) {
            case "MODIFICATION_NOTE" -> {
                ajouterInscription(contexte, id(d, "inscriptionId"));
                ajouterEvaluation(contexte, id(d, "evaluationId"));
                yield "Note modifiée : " + valeurNote(texte(d, "avant")) + " → " + valeurNote(texte(d, "apres"));
            }
            case "SUPPRESSION_NOTE" -> {
                ajouterInscription(contexte, id(d, "inscriptionId"));
                ajouterEvaluation(contexte, id(d, "evaluationId"));
                yield "Note supprimée (elle valait " + valeurNote(texte(d, "avant")) + ")";
            }
            case "ENCAISSEMENT" -> {
                ajouterFactureDuPaiement(contexte, entiteId);
                yield "Encaissement de " + nombre(texte(d, "montant")) + " (" + libelleMode(texte(d, "mode"))
                        + ") sur la facture " + texte(d, "facture");
            }
            case "INITIATION_PAIEMENT" -> {
                ajouterFactureDuPaiement(contexte, entiteId);
                yield "Paiement en ligne initié : " + nombre(texte(d, "montant")) + " sur la facture " + texte(d, "facture");
            }
            case "PAIEMENT_VALIDE" -> {
                ajouterFactureDuPaiement(contexte, entiteId);
                yield "Paiement en ligne validé : " + nombre(texte(d, "montant"));
            }
            case "PAIEMENT_SURPAIEMENT" -> {
                ajouterFactureDuPaiement(contexte, entiteId);
                yield "Paiement en ligne validé avec surpaiement : " + nombre(texte(d, "montant"));
            }
            case "PAIEMENT_ECHEC" -> {
                ajouterFactureDuPaiement(contexte, entiteId);
                yield "Paiement en ligne échoué (référence " + texte(d, "reference") + ")";
            }
            case "PAIEMENT_MONTANT_INCOHERENT" -> {
                ajouterFactureDuPaiement(contexte, entiteId);
                yield "Notification de paiement refusée : montant attendu " + nombre(texte(d, "attendu"))
                        + ", reçu " + nombre(texte(d, "recu"));
            }
            case "GENERATION_FACTURES" -> {
                ajouterEtablissement(contexte, id(d, "etablissementId"));
                ajouterClasse(contexte, id(d, "classeId"));
                yield texte(d, "creees") + " facture(s) générée(s), " + texte(d, "ignorees") + " déjà existante(s)";
            }
            case "EXONERATION_FACTURE" -> {
                ajouterFacture(contexte, entiteId);
                yield "Exonération de " + nombre(texte(d, "montant")) + " (" + minuscule(texte(d, "type")) + ") : "
                        + texte(d, "motif");
            }
            case "ANNULATION_FACTURE" -> {
                ajouterFacture(contexte, entiteId);
                yield "Facture " + texte(d, "numero") + " annulée (" + nombre(texte(d, "montantDu")) + ")";
            }
            case "CREATION_FRAIS" -> {
                ajouterFrais(contexte, entiteId);
                yield "Frais « " + texte(d, "libelle") + " » créé : " + nombre(texte(d, "montant"));
            }
            case "MODIFICATION_FRAIS" -> {
                ajouterFrais(contexte, entiteId);
                yield "Montant du frais modifié : " + nombre(texte(d, "ancienMontant")) + " → "
                        + nombre(texte(d, "nouveauMontant"));
            }
            case "DESACTIVATION_FRAIS" -> {
                ajouterFrais(contexte, entiteId);
                yield "Frais « " + texte(d, "libelle") + " » désactivé";
            }
            case "CREATION_ZONE" -> "Zone « " + texte(d, "nom") + " » créée";
            case "MODIFICATION_ZONE" -> "Zone « " + texte(d, "nom") + " » modifiée";
            case "SUPPRESSION_ZONE" -> "Zone « " + texte(d, "nom") + " » supprimée";
            case "CREATION_ETABLISSEMENT" -> {
                ajouterEtablissement(contexte, entiteId);
                yield "Établissement « " + texte(d, "nom") + " » créé (code " + texte(d, "code") + ")";
            }
            case "MODIFICATION_ETABLISSEMENT" -> {
                ajouterEtablissement(contexte, entiteId);
                yield "Établissement « " + texte(d, "nom") + " » modifié";
            }
            case "CHANGEMENT_STATUT_ETABLISSEMENT" -> {
                ajouterEtablissement(contexte, entiteId);
                yield "Statut de l'établissement : " + texte(d, "avant") + " → " + texte(d, "apres");
            }
            case "CREATION_COMPTE_DIRECTION" -> {
                ajouterEtablissement(contexte, id(d, "etablissementId"));
                ajouterCompte(contexte, entiteId);
                yield "Compte de direction créé : " + texte(d, "email");
            }
            case "CREATION_COMPTE_PERSONNEL" -> {
                ajouterEtablissement(contexte, id(d, "etablissementId"));
                ajouterCompte(contexte, entiteId);
                yield "Compte de personnel administratif créé : " + texte(d, "email");
            }
            case "CREATION_COMPTE_DIRECTION_REGIONALE" -> {
                ajouterZone(contexte, id(d, "zoneId"));
                ajouterCompte(contexte, entiteId);
                yield "Compte de direction régionale créé : " + texte(d, "email");
            }
            case "REINITIALISATION_MOT_DE_PASSE" -> {
                ajouterCompte(contexte, entiteId);
                yield "Mot de passe réinitialisé pour " + texte(d, "email");
            }
            default -> libelleParDefaut(action);
        };
        return new Description(resume, contexte);
    }

    // ---------- Éléments de contexte ----------

    private void ajouterInscription(List<ElementContexte> contexte, Long inscriptionId) {
        trouver(inscriptionId, inscriptionRepository::findById).ifPresent((Inscription i) -> {
            ajouterEtablissement(contexte, i.getEtablissement());
            ajouterEleve(contexte, i.getEleve());
            Classe classe = i.getClasse();
            if (classe != null) {
                contexte.add(new ElementContexte("Classe", classe.getNom()));
            }
        });
    }

    private void ajouterEvaluation(List<ElementContexte> contexte, Long evaluationId) {
        trouver(evaluationId, evaluationRepository::findById).ifPresent((Evaluation e) ->
                contexte.add(new ElementContexte("Évaluation", e.getTitre() + " (" + e.getMatiere().getLibelle() + ")")));
    }

    private void ajouterFactureDuPaiement(List<ElementContexte> contexte, Long paiementId) {
        trouver(paiementId, paiementRepository::findById)
                .ifPresent(p -> ajouterFacture(contexte, p.getFacture()));
    }

    private void ajouterFacture(List<ElementContexte> contexte, Long factureId) {
        trouver(factureId, factureRepository::findById).ifPresent(f -> ajouterFacture(contexte, f));
    }

    private void ajouterFacture(List<ElementContexte> contexte, Facture f) {
        ajouterEtablissement(contexte, f.getEtablissement());
        ajouterEleve(contexte, f.getEleve());
        contexte.add(new ElementContexte("Facture", f.getNumero() + " — " + f.getLibelle()));
    }

    private void ajouterFrais(List<ElementContexte> contexte, Long fraisId) {
        trouver(fraisId, fraisRepository::findById).ifPresent(f -> {
            ajouterEtablissement(contexte, f.getEtablissement());
            contexte.add(new ElementContexte("Frais", f.getLibelle()));
        });
    }

    private void ajouterEtablissement(List<ElementContexte> contexte, Long etablissementId) {
        trouver(etablissementId, etablissementRepository::findById).ifPresent(e -> ajouterEtablissement(contexte, e));
    }

    private void ajouterEtablissement(List<ElementContexte> contexte, Etablissement e) {
        if (e != null) {
            contexte.add(new ElementContexte("Établissement", e.getNom() + " (" + e.getCodeOfficiel() + ")"));
        }
    }

    private void ajouterEleve(List<ElementContexte> contexte, Eleve e) {
        if (e != null) {
            contexte.add(new ElementContexte("Élève", e.getPrenom() + " " + e.getNom() + " (" + e.getMatricule() + ")"));
        }
    }

    private void ajouterClasse(List<ElementContexte> contexte, Long classeId) {
        trouver(classeId, classeRepository::findById)
                .ifPresent(c -> contexte.add(new ElementContexte("Classe", c.getNom())));
    }

    private void ajouterZone(List<ElementContexte> contexte, Long zoneId) {
        trouver(zoneId, zoneRepository::findById)
                .ifPresent(z -> contexte.add(new ElementContexte("Zone", z.getNom())));
    }

    private void ajouterCompte(List<ElementContexte> contexte, Long utilisateurId) {
        trouver(utilisateurId, utilisateurRepository::findById).ifPresent((Utilisateur u) ->
                contexte.add(new ElementContexte("Compte", u.getPrenom() + " " + u.getNom() + " — " + u.getEmail())));
    }

    // ---------- Utilitaires ----------

    private static <T> Optional<T> trouver(Long id, java.util.function.Function<Long, Optional<T>> recherche) {
        return id == null ? Optional.empty() : recherche.apply(id);
    }

    static Long id(Map<String, Object> details, String cle) {
        Object v = details == null ? null : details.get(cle);
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v instanceof String s && s.matches("\\d+")) {
            return Long.valueOf(s);
        }
        return null;
    }

    static String texte(Map<String, Object> details, String cle) {
        Object v = details == null ? null : details.get(cle);
        return v == null ? null : v.toString();
    }

    /** "14.00" -> "14,00" ; "ABSENT" -> "absent" ; null -> "aucune note". */
    static String valeurNote(String valeur) {
        if (valeur == null) {
            return "aucune note";
        }
        return "ABSENT".equals(valeur) ? "absent" : valeur.replace('.', ',');
    }

    static String nombre(String valeur) {
        return valeur == null ? "?" : valeur.replace('.', ',');
    }

    static String libelleMode(String mode) {
        if (mode == null) {
            return "mode inconnu";
        }
        return switch (mode) {
            case "ESPECES" -> "espèces";
            case "CHEQUE" -> "chèque";
            case "VIREMENT" -> "virement";
            default -> minuscule(mode);
        };
    }

    static String minuscule(String valeur) {
        return valeur == null ? "" : valeur.toLowerCase().replace('_', ' ');
    }

    /** "CREATION_COMPTE_X" -> "Creation compte x" quand aucune phrase n'est prévue. */
    static String libelleParDefaut(String action) {
        String texte = action.toLowerCase().replace('_', ' ');
        return texte.isEmpty() ? action : Character.toUpperCase(texte.charAt(0)) + texte.substring(1);
    }
}
