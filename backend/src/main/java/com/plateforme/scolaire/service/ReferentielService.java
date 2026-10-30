package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Niveau;
import com.plateforme.scolaire.domain.Periode;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.CycleScolaireRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.repository.NiveauRepository;
import com.plateforme.scolaire.repository.PeriodeRepository;
import com.plateforme.scolaire.web.dto.AnneeScolaireDto;
import com.plateforme.scolaire.web.dto.AnneeScolaireRequest;
import com.plateforme.scolaire.web.dto.MatiereDto;
import com.plateforme.scolaire.web.dto.MatiereRequest;
import com.plateforme.scolaire.web.dto.NiveauDto;
import com.plateforme.scolaire.web.dto.NiveauRequest;
import com.plateforme.scolaire.web.dto.PeriodeDto;
import com.plateforme.scolaire.web.dto.PeriodeRequest;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Données de configuration nationales : années, périodes, niveaux, matières. */
@Service
@RequiredArgsConstructor
@Transactional
public class ReferentielService {

    private final AnneeScolaireRepository anneeRepository;
    private final PeriodeRepository periodeRepository;
    private final NiveauRepository niveauRepository;
    private final MatiereRepository matiereRepository;
    private final CycleScolaireRepository cycleRepository;

    // ---------- Années scolaires ----------

    @Transactional(readOnly = true)
    public List<AnneeScolaireDto> annees() {
        return anneeRepository.findAllByOrderByDateDebutDesc().stream().map(AnneeScolaireDto::de).toList();
    }

    public AnneeScolaireDto creerAnnee(AnneeScolaireRequest r) {
        verifierDates(r.dateDebut(), r.dateFin());
        if (anneeRepository.findByLibelle(r.libelle()).isPresent()) {
            throw conflit("Une année scolaire porte déjà ce libellé");
        }
        return AnneeScolaireDto.de(anneeRepository.save(appliquer(new AnneeScolaire(), r)));
    }

    public AnneeScolaireDto modifierAnnee(Long id, AnneeScolaireRequest r) {
        AnneeScolaire annee = anneeRepository.findById(id)
                .orElseThrow(() -> introuvable("Année scolaire introuvable"));
        verifierDates(r.dateDebut(), r.dateFin());
        anneeRepository.findByLibelle(r.libelle()).filter(autre -> !autre.getId().equals(id))
                .ifPresent(autre -> {
                    throw conflit("Une année scolaire porte déjà ce libellé");
                });
        return AnneeScolaireDto.de(appliquer(annee, r));
    }

    private AnneeScolaire appliquer(AnneeScolaire a, AnneeScolaireRequest r) {
        a.setLibelle(r.libelle());
        a.setDateDebut(r.dateDebut());
        a.setDateFin(r.dateFin());
        return a;
    }

    // ---------- Périodes (trimestres, semestres...) ----------

    @Transactional(readOnly = true)
    public List<PeriodeDto> periodes(Long anneeId) {
        if (!anneeRepository.existsById(anneeId)) {
            throw introuvable("Année scolaire introuvable");
        }
        return periodeRepository.findByAnneeScolaireIdOrderByOrdre(anneeId).stream().map(PeriodeDto::de).toList();
    }

    public PeriodeDto creerPeriode(Long anneeId, PeriodeRequest r) {
        AnneeScolaire annee = anneeRepository.findById(anneeId)
                .orElseThrow(() -> introuvable("Année scolaire introuvable"));
        verifierDates(r.dateDebut(), r.dateFin());
        if (r.dateDebut().isBefore(annee.getDateDebut()) || r.dateFin().isAfter(annee.getDateFin())) {
            throw invalide("La période doit être comprise dans l'année scolaire");
        }
        if (periodeRepository.existsByAnneeScolaireIdAndOrdre(anneeId, r.ordre())) {
            throw conflit("Une période de cette année a déjà ce numéro d'ordre");
        }
        Periode p = new Periode();
        p.setAnneeScolaire(annee);
        p.setLibelle(r.libelle());
        p.setOrdre(r.ordre());
        p.setDateDebut(r.dateDebut());
        p.setDateFin(r.dateFin());
        return PeriodeDto.de(periodeRepository.save(p));
    }

    public PeriodeDto modifierPeriode(Long id, PeriodeRequest r) {
        Periode p = periodeRepository.findById(id).orElseThrow(() -> introuvable("Période introuvable"));
        AnneeScolaire annee = p.getAnneeScolaire();
        verifierDates(r.dateDebut(), r.dateFin());
        if (r.dateDebut().isBefore(annee.getDateDebut()) || r.dateFin().isAfter(annee.getDateFin())) {
            throw invalide("La période doit être comprise dans l'année scolaire");
        }
        boolean ordrePris = periodeRepository.findByAnneeScolaireIdOrderByOrdre(annee.getId()).stream()
                .anyMatch(autre -> !autre.getId().equals(id) && autre.getOrdre() == r.ordre());
        if (ordrePris) {
            throw conflit("Une période de cette année a déjà ce numéro d'ordre");
        }
        p.setLibelle(r.libelle());
        p.setOrdre(r.ordre());
        p.setDateDebut(r.dateDebut());
        p.setDateFin(r.dateFin());
        return PeriodeDto.de(p);
    }

    // ---------- Niveaux ----------

    @Transactional(readOnly = true)
    public List<NiveauDto> niveaux() {
        return niveauRepository.findAllByOrderByOrdre().stream().map(NiveauDto::de).toList();
    }

    public NiveauDto creerNiveau(NiveauRequest r) {
        if (niveauRepository.findByCode(r.code()).isPresent()) {
            throw conflit("Un niveau porte déjà ce code");
        }
        return NiveauDto.de(niveauRepository.save(appliquer(new Niveau(), r)));
    }

    public NiveauDto modifierNiveau(Long id, NiveauRequest r) {
        Niveau niveau = niveauRepository.findById(id).orElseThrow(() -> introuvable("Niveau introuvable"));
        niveauRepository.findByCode(r.code()).filter(autre -> !autre.getId().equals(id))
                .ifPresent(autre -> {
                    throw conflit("Un niveau porte déjà ce code");
                });
        return NiveauDto.de(appliquer(niveau, r));
    }

    private Niveau appliquer(Niveau n, NiveauRequest r) {
        n.setCode(r.code());
        n.setLibelle(r.libelle());
        n.setOrdre(r.ordre());
        n.setCycle(r.cycleId() == null ? null : cycleRepository.findById(r.cycleId())
                .orElseThrow(() -> invalide("Cycle inconnu")));
        return n;
    }

    // ---------- Matières ----------

    @Transactional(readOnly = true)
    public List<MatiereDto> matieres() {
        return matiereRepository.findAllByOrderByLibelle().stream().map(MatiereDto::de).toList();
    }

    public MatiereDto creerMatiere(MatiereRequest r) {
        if (matiereRepository.findByCode(r.code()).isPresent()) {
            throw conflit("Une matière porte déjà ce code");
        }
        return MatiereDto.de(matiereRepository.save(appliquer(new Matiere(), r)));
    }

    public MatiereDto modifierMatiere(Long id, MatiereRequest r) {
        Matiere matiere = matiereRepository.findById(id).orElseThrow(() -> introuvable("Matière introuvable"));
        matiereRepository.findByCode(r.code()).filter(autre -> !autre.getId().equals(id))
                .ifPresent(autre -> {
                    throw conflit("Une matière porte déjà ce code");
                });
        return MatiereDto.de(appliquer(matiere, r));
    }

    private Matiere appliquer(Matiere m, MatiereRequest r) {
        m.setCode(r.code());
        m.setLibelle(r.libelle());
        return m;
    }

    // ---------- Erreurs ----------

    private static void verifierDates(LocalDate debut, LocalDate fin) {
        if (!fin.isAfter(debut)) {
            throw invalide("La date de fin doit être postérieure à la date de début");
        }
    }

    private static ResponseStatusException conflit(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private static ResponseStatusException introuvable(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException invalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
