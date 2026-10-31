package com.plateforme.scolaire.dev;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.Inspection;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.ResultatExamen;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.SignalementSecurite;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.InspectionRepository;
import com.plateforme.scolaire.repository.ResultatExamenRepository;
import com.plateforme.scolaire.repository.SignalementSecuriteRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.repository.ZoneGeographiqueRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Données de démonstration pour les écrans du ministère : un compte de direction régionale, des autorisations, des
 * inspections, un signalement de sécurité, un examen national et des postes d'enseignants.
 */
@Component
@Profile("dev")
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class DevConformiteInitializer implements CommandLineRunner {

    static final String EMAIL_REGION = "region1@example.org";

    private final EtablissementRepository etablissementRepository;
    private final ZoneGeographiqueRepository zoneRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AffectationRoleRepository affectationRoleRepository;
    private final InspectionRepository inspectionRepository;
    private final SignalementSecuriteRepository signalementRepository;
    private final AnneeScolaireRepository anneeRepository;
    private final ExamenNationalRepository examenRepository;
    private final ResultatExamenRepository resultatRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.dev.mot-de-passe}")
    private String motDePasse;

    @Override
    @Transactional
    public void run(String... args) {
        if (utilisateurRepository.existsByEmailIgnoreCase(EMAIL_REGION)) {
            return;
        }
        Etablissement ecole1 = etablissementRepository.findAll().stream()
                .filter(e -> "ECOLE-DEMO-1".equals(e.getCodeOfficiel())).findFirst().orElse(null);
        Etablissement ecole2 = etablissementRepository.findAll().stream()
                .filter(e -> "ECOLE-DEMO-2".equals(e.getCodeOfficiel())).findFirst().orElse(null);
        if (ecole1 == null || ecole2 == null || ecole1.getZone() == null) {
            return;
        }

        Utilisateur region = new Utilisateur();
        region.setEmail(EMAIL_REGION);
        region.setNom("Région");
        region.setPrenom("Démo");
        region.setMotDePasseHash(passwordEncoder.encode(motDePasse));
        utilisateurRepository.save(region);
        AffectationRole role = new AffectationRole();
        role.setUtilisateur(region);
        role.setRole(Role.DIRECTION_REGIONALE);
        role.setPerimetre(Perimetre.ZONE);
        role.setZone(ecole1.getZone());
        affectationRoleRepository.save(role);

        LocalDate aujourdhui = LocalDate.now();
        ecole1.setAutorisationStatut("AUTORISE");
        ecole1.setAutorisationReference("Arrêté démo 001");
        ecole1.setAutorisationDate(aujourdhui.minusYears(3));
        ecole1.setPostesEnseignants(3);
        // L'école 2 fonctionne sans autorisation et a un problème grave : elle ressort « urgente ».
        ecole2.setAutorisationStatut("RETIRE");
        ecole2.setMotifStatut("Locaux non conformes");
        ecole2.setPostesEnseignants(2);

        inspection(ecole1, region, aujourdhui.minusDays(40), "CONFORME", "Rien à signaler.");
        inspection(ecole2, region, aujourdhui.minusDays(20), "NON_CONFORME", "Salles de classe en mauvais état.");
        SignalementSecurite s = new SignalementSecurite();
        s.setEtablissement(ecole2);
        s.setSignalePar(region);
        s.setCategorie("INCENDIE");
        s.setGravite("ELEVEE");
        s.setDescription("Aucun extincteur dans le bâtiment principal.");
        s.setCreatedAt(Instant.now());
        signalementRepository.save(s);

        anneeRepository.findAllByOrderByDateDebutDesc().stream().findFirst().ifPresent(annee -> {
            ExamenNational examen = new ExamenNational();
            examen.setLibelle("Examen démo");
            examen.setAnneeScolaire(annee);
            examenRepository.save(examen);
            resultat(examen, ecole1, 60, 51);
            resultat(examen, ecole2, 40, 22);
        });
        log.info("Données de conformité de démonstration créées (compte {} ; mot de passe de démonstration)", EMAIL_REGION);
    }

    private void inspection(Etablissement e, Utilisateur inspecteur, LocalDate date, String resultat, String observations) {
        Inspection i = new Inspection();
        i.setEtablissement(e);
        i.setInspecteur(inspecteur);
        i.setDateInspection(date);
        i.setResultat(resultat);
        i.setObservations(observations);
        i.setCreatedAt(date.atStartOfDay().toInstant(ZoneOffset.UTC));
        inspectionRepository.save(i);
    }

    private void resultat(ExamenNational examen, Etablissement e, int presentes, int admis) {
        ResultatExamen r = new ResultatExamen();
        r.setExamen(examen);
        r.setEtablissement(e);
        r.setPresentes(presentes);
        r.setAdmis(admis);
        resultatRepository.save(r);
    }
}
