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
            case "ANNULATION_PAIEMENT" -> {
                ajouterFactureDuPaiement(contexte, entiteId);
                yield "Paiement annulé : " + nombre(texte(d, "montant")) + " (" + libelleMode(texte(d, "mode"))
                        + ") sur la facture " + texte(d, "facture") + " — motif : " + texte(d, "motif");
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
            case "PAIEMENT_EXPIRE" -> {
                ajouterFactureDuPaiement(contexte, entiteId);
                yield "Paiement en ligne expiré sans réponse du prestataire (référence " + texte(d, "reference") + ")";
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
            case "DEMANDE_INSCRIPTION" -> {
                ajouterInscription(contexte, entiteId);
                yield Boolean.TRUE.equals(booleen(d, "nouvelEleve"))
                        ? "Inscription demandée pour un nouvel élève" : "Inscription demandée pour un élève existant";
            }
            case "ACCEPTATION_INSCRIPTION" -> {
                ajouterInscription(contexte, entiteId);
                yield "Inscription acceptée";
            }
            case "REFUS_INSCRIPTION" -> {
                ajouterInscription(contexte, entiteId);
                yield "Inscription refusée";
            }
            case "ANNULATION_INSCRIPTION" -> {
                ajouterInscription(contexte, entiteId);
                yield "Inscription annulée (elle était " + statutInscription(texte(d, "statutAvant")) + ")";
            }
            case "SAISIE_APPEL" -> {
                ajouterClasse(contexte, id(d, "classeId"));
                yield "Appel du " + dateFrancaise(texte(d, "date")) + " : " + texte(d, "presents") + " présent(s), "
                        + texte(d, "absents") + " absent(s), " + texte(d, "retards") + " retard(s)"
                        + ("0".equals(texte(d, "corrigees")) ? "" : " — " + texte(d, "corrigees") + " correction(s)");
            }
            case "CORRECTION_PRESENCE" -> {
                ajouterInscription(contexte, id(d, "inscriptionId"));
                yield "Présence du " + dateFrancaise(texte(d, "date")) + " corrigée : " + libellePresence(texte(d, "avant"))
                        + " → " + libellePresence(texte(d, "apres"));
            }
            case "MODIFICATION_COEFFICIENTS" -> {
                ajouterClasse(contexte, id(d, "classeId"));
                yield "Coefficients des matières modifiés : " + resumeChangements(d);
            }
            case "DEMANDE_REINITIALISATION_MDP" -> {
                ajouterCompte(contexte, entiteId);
                yield "Réinitialisation du mot de passe demandée par e-mail";
            }
            case "MOT_DE_PASSE_REINITIALISE_PAR_EMAIL" -> {
                ajouterCompte(contexte, entiteId);
                yield "Mot de passe réinitialisé via le lien reçu par e-mail";
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

    /** {"Maths": "1 → 3"} -> "Maths 1 → 3, …". */
    static String resumeChangements(Map<String, Object> details) {
        Object brut = details == null ? null : details.get("changements");
        if (!(brut instanceof Map<?, ?> changements) || changements.isEmpty()) {
            return "aucun détail";
        }
        return changements.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(java.util.stream.Collectors.joining(", "));
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

    /** "PRESENT" -> "présent" ; "ABSENT (justifié)" -> "absent (justifié)". */
    static String libellePresence(String valeur) {
        return minuscule(valeur).replace("present", "présent");
    }

    static Boolean booleen(Map<String, Object> details, String cle) {
        Object v = details == null ? null : details.get(cle);
        return v instanceof Boolean b ? b : null;
    }

    /** "2026-10-26" -> "26/10/2026". */
    static String dateFrancaise(String iso) {
        if (iso == null || !iso.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return iso == null ? "?" : iso;
        }
        return iso.substring(8) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    static String statutInscription(String statut) {
        if (statut == null) {
            return "dans un état inconnu";
        }
        return switch (statut) {
            case "EN_ATTENTE" -> "en attente";
            case "ACCEPTEE" -> "acceptée";
            default -> minuscule(statut);
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
