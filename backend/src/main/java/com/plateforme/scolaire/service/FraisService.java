package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Facture;
import com.plateforme.scolaire.domain.Frais;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.Niveau;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.FactureRepository;
import com.plateforme.scolaire.repository.FraisRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.NiveauRepository;
import com.plateforme.scolaire.web.dto.FraisDto;
import com.plateforme.scolaire.web.dto.FraisRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Grille tarifaire d'un établissement (Étape 3a).
 * Les autorisations (@PreAuthorize) sont vérifiées dans les contrôleurs ; ce service applique
 * les règles métier.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FraisService {

    private final FraisRepository fraisRepository;
    private final EtablissementRepository etablissementRepository;
    private final AnneeScolaireRepository anneeRepository;
    private final NiveauRepository niveauRepository;
    private final ClasseRepository classeRepository;
    private final InscriptionRepository inscriptionRepository;
    private final FactureRepository factureRepository;
    private final AuditService auditService;

    // ---------- Consultation ----------

    @Transactional(readOnly = true)
    public List<FraisDto> lister(Long etablissementId, Long anneeScolaireId) {
        if (!etablissementRepository.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        if (anneeScolaireId == null) {
            return fraisRepository.findAll().stream()
                    .filter(f -> f.getEtablissement().getId().equals(etablissementId))
                    .map(FraisDto::de)
                    .toList();
        }
        return fraisRepository.findByEtablissementIdAndAnneeScolaireIdOrderByLibelle(etablissementId, anneeScolaireId)
                .stream().map(FraisDto::de).toList();
    }

    @Transactional(readOnly = true)
    public FraisDto trouver(Long id) {
        return FraisDto.de(charger(id));
    }

    // ---------- Écriture ----------

    public FraisDto creer(Long etablissementId, FraisRequest r) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        AnneeScolaire annee = anneeRepository.findById(r.anneeScolaireId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année scolaire inconnue"));
        Niveau niveau = r.niveauId() == null ? null : niveauRepository.findById(r.niveauId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niveau inconnu"));

        String libelle = r.libelle().trim();
        Optional<Frais> existant = niveau == null
                ? fraisRepository.findByEtablissementIdAndAnneeScolaireIdAndNiveauIsNullAndLibelleIgnoreCase(
                        etablissementId, annee.getId(), libelle)
                : fraisRepository.findByEtablissementIdAndAnneeScolaireIdAndNiveauIdAndLibelleIgnoreCase(
                        etablissementId, annee.getId(), niveau.getId(), libelle);
        if (existant.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un frais portant ce libellé existe déjà pour cette année"
                            + (niveau == null ? "" : " et ce niveau"));
        }

        Frais frais = new Frais();
        frais.setEtablissement(etablissement);
        frais.setAnneeScolaire(annee);
        frais.setNiveau(niveau);
        frais.setLibelle(libelle);
        frais.setMontant(r.montant());
        frais.setDevise(r.devise() == null || r.devise().isBlank() ? "XOF" : r.devise());
        frais.setEcheance(r.echeance());
        frais.setObligatoire(r.obligatoire() == null || r.obligatoire());
        frais.setActif(r.actif() == null || r.actif());
        Frais enregistre = fraisRepository.save(frais);

        auditService.enregistrer("CREATION_FRAIS", "frais", enregistre.getId(),
                java.util.Map.of("libelle", libelle, "montant", r.montant().toString()));
        return FraisDto.de(enregistre);
    }

    /** Modifie la grille. Les factures déjà émises conservent leur montant d'origine. */
    public FraisDto modifier(Long id, FraisRequest r) {
        Frais frais = charger(id);
        Niveau niveau = r.niveauId() == null ? null : niveauRepository.findById(r.niveauId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niveau inconnu"));
        String libelle = r.libelle().trim();
        Optional<Frais> existant = niveau == null
                ? fraisRepository.findByEtablissementIdAndAnneeScolaireIdAndNiveauIsNullAndLibelleIgnoreCase(
                        frais.getEtablissement().getId(), frais.getAnneeScolaire().getId(), libelle)
                : fraisRepository.findByEtablissementIdAndAnneeScolaireIdAndNiveauIdAndLibelleIgnoreCase(
                        frais.getEtablissement().getId(), frais.getAnneeScolaire().getId(), niveau.getId(), libelle);
        if (existant.isPresent() && !existant.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un autre frais porte déjà ce libellé pour cette année");
        }

        BigDecimal ancienMontant = frais.getMontant();
        frais.setNiveau(niveau);
        frais.setLibelle(libelle);
        frais.setMontant(r.montant());
        if (r.devise() != null && !r.devise().isBlank()) {
            frais.setDevise(r.devise());
        }
        frais.setEcheance(r.echeance());
        if (r.obligatoire() != null) {
            frais.setObligatoire(r.obligatoire());
        }
        if (r.actif() != null) {
            frais.setActif(r.actif());
        }

        auditService.enregistrer("MODIFICATION_FRAIS", "frais", frais.getId(),
                java.util.Map.of("ancienMontant", ancienMontant.toString(),
                        "nouveauMontant", r.montant().toString()));
        return FraisDto.de(frais);
    }

    /** Désactive un frais : il ne sera plus proposé à la génération, les factures existantes restent. */
    public void desactiver(Long id) {
        Frais frais = charger(id);
        frais.setActif(false);
        auditService.enregistrer("DESACTIVATION_FRAIS", "frais", frais.getId(),
                java.util.Map.of("libelle", frais.getLibelle()));
    }

    // ---------- Interne ----------

    private Frais charger(Long id) {
        return fraisRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Frais introuvable"));
    }

    /** Élèves d'une classe pour une année (inscriptions acceptées). */
    @Transactional(readOnly = true)
    public List<Inscription> elevesDeLaClasse(Long classeId) {
        return inscriptionRepository.findByClasseIdAndStatutOrderByEleveNomAscElevePrenomAsc(classeId, "ACCEPTEE");
    }

    @Transactional(readOnly = true)
    public Classe chargerClasse(Long classeId) {
        return classeRepository.findById(classeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
    }
}
