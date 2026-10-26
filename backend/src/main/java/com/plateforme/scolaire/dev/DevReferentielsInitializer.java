package com.plateforme.scolaire.dev;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Niveau;
import com.plateforme.scolaire.domain.Periode;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.repository.NiveauRepository;
import com.plateforme.scolaire.repository.PeriodeRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Référentiels génériques de démonstration (profil dev), créés uniquement s'ils n'existent pas. */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevReferentielsInitializer implements CommandLineRunner {

    private final AnneeScolaireRepository anneeRepository;
    private final PeriodeRepository periodeRepository;
    private final NiveauRepository niveauRepository;
    private final MatiereRepository matiereRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (anneeRepository.count() > 0) {
            return;
        }
        AnneeScolaire annee = new AnneeScolaire();
        annee.setLibelle("2026-2027");
        annee.setDateDebut(LocalDate.of(2026, 9, 1));
        annee.setDateFin(LocalDate.of(2027, 6, 30));
        anneeRepository.save(annee);

        periode(annee, "Période 1", 1, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20));
        periode(annee, "Période 2", 2, LocalDate.of(2027, 1, 5), LocalDate.of(2027, 3, 31));
        periode(annee, "Période 3", 3, LocalDate.of(2027, 4, 10), LocalDate.of(2027, 6, 30));

        niveau("N1", "Niveau 1", 1);
        niveau("N2", "Niveau 2", 2);
        niveau("N3", "Niveau 3", 3);

        matiere("LANGUE", "Langue");
        matiere("MATHS", "Mathématiques");
        matiere("SCIENCES", "Sciences");

        log.info("Référentiels de démonstration créés (année 2026-2027, 3 périodes, 3 niveaux, 3 matières)");
    }

    private void periode(AnneeScolaire annee, String libelle, int ordre, LocalDate debut, LocalDate fin) {
        Periode p = new Periode();
        p.setAnneeScolaire(annee);
        p.setLibelle(libelle);
        p.setOrdre(ordre);
        p.setDateDebut(debut);
        p.setDateFin(fin);
        periodeRepository.save(p);
    }

    private void niveau(String code, String libelle, int ordre) {
        Niveau n = new Niveau();
        n.setCode(code);
        n.setLibelle(libelle);
        n.setOrdre(ordre);
        niveauRepository.save(n);
    }

    private void matiere(String code, String libelle) {
        Matiere m = new Matiere();
        m.setCode(code);
        m.setLibelle(libelle);
        matiereRepository.save(m);
    }
}
