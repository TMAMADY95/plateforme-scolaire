package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Facture;
import com.plateforme.scolaire.domain.Paiement;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.FactureRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.repository.PaiementRepository;
import com.plateforme.scolaire.security.PerimetreFinancesService;
import com.plateforme.scolaire.web.dto.BulletinDto;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Bulletin et reçu en PDF. Les droits d'accès sont ceux des données correspondantes. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentsPdfService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MoyennesService moyennesService;
    private final ClasseRepository classeRepository;
    private final FactureRepository factureRepository;
    private final PaiementRepository paiementRepository;
    private final EleveRepository eleveRepository;
    private final LienParentalRepository lienParentalRepository;
    private final PerimetreFinancesService perimetreFinances;
    private final GenerateurPdf generateur;
    private final Clock clock;

    /** L'accès à l'élève est vérifié par le contrôleur (comme pour le bulletin JSON). */
    public byte[] bulletin(Long eleveId, Long periodeId) {
        BulletinDto b = moyennesService.bulletin(eleveId, periodeId);
        Classe classe = classeRepository.findById(b.classeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
        return generateur.bulletin(new GenerateurPdf.DonneesBulletin(
                classe.getEtablissement().getNom(), classe.getAnneeScolaire().getLibelle(), b.classeNom(), b.periode(),
                b.eleve().prenom() + " " + b.eleve().nom(), b.eleve().matricule(),
                b.matieres().stream()
                        .map(l -> new GenerateurPdf.LigneBulletin(l.matiere(), l.moyenneEleve(), l.moyenneClasse(), l.coefficient()))
                        .toList(),
                b.moyenneGenerale(), b.moyenneGeneraleClasse(), b.rang(), b.effectif()));
    }

    /**
     * Reçu d'une facture : direction et personnel administratif, élève concerné ou ses parents.
     * Même réponse 404 qu'une facture inexistante pour qui n'y a pas droit.
     */
    public byte[] recu(Long utilisateurId, Long factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));
        boolean autorise = perimetreFinances.peutAccederFacture(factureId)
                || estProprietaire(utilisateurId, facture.getEleve().getId());
        if (!autorise) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable");
        }

        List<Paiement> valides = paiementRepository.findByFactureIdOrderByCreatedAtDescIdDesc(factureId).stream()
                .filter(p -> "VALIDE".equals(p.getStatut()))
                .sorted(Comparator.comparing(Paiement::getCreatedAt))
                .toList();
        if (valides.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucun paiement validé pour cette facture");
        }

        ZoneId zone = clock.getZone();
        BigDecimal total = valides.stream().map(Paiement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reste = facture.getMontantDu().subtract(facture.getMontantExonere()).subtract(total).max(BigDecimal.ZERO);
        List<GenerateurPdf.LignePaiement> lignes = valides.stream()
                .map(p -> new GenerateurPdf.LignePaiement(
                        (p.getValideLe() != null ? p.getValideLe() : p.getCreatedAt()).atZone(zone).format(JOUR),
                        libelleMode(p.getMode()), p.getReferenceInterne(), p.getMontant()))
                .toList();
        return generateur.recu(new GenerateurPdf.DonneesRecu(
                facture.getEtablissement().getNom(), facture.getNumero(), facture.getLibelle(),
                facture.getEleve().getPrenom() + " " + facture.getEleve().getNom(), facture.getEleve().getMatricule(),
                facture.getDevise(), facture.getMontantDu(), facture.getMontantExonere(), lignes, total, reste,
                LocalDate.now(clock).format(JOUR)));
    }

    private boolean estProprietaire(Long utilisateurId, Long eleveId) {
        boolean sonDossier = eleveRepository.findByUtilisateurId(utilisateurId)
                .map(e -> e.getId().equals(eleveId)).orElse(false);
        return sonDossier || lienParentalRepository.existsByEleveIdAndUtilisateurId(eleveId, utilisateurId);
    }

    static String libelleMode(String mode) {
        return switch (mode) {
            case "ESPECES" -> "Espèces";
            case "VIREMENT" -> "Virement";
            case "CHEQUE" -> "Chèque";
            case "MOBILE_MONEY" -> "Mobile money";
            default -> mode;
        };
    }
}
