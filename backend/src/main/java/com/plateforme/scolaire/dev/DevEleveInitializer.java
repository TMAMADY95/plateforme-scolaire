package com.plateforme.scolaire.dev;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.Niveau;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.NiveauRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import java.time.LocalDate;
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
 * Crée un compte élève de démonstration (profil dev), lié à un dossier élève inscrit dans une classe.
 * Doit s'exécuter APRÈS DevReferentielsInitializer (@Order(1)) et DevDataInitializer (@Order(2)).
 */
@Component
@Profile("dev")
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class DevEleveInitializer implements CommandLineRunner {

    private static final String EMAIL_ELEVE = "eleve1@example.org";

    private final UtilisateurRepository utilisateurRepository;
    private final AffectationRoleRepository affectationRoleRepository;
    private final EleveRepository eleveRepository;
    private final ClasseRepository classeRepository;
    private final InscriptionRepository inscriptionRepository;
    private final EtablissementRepository etablissementRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;
    private final NiveauRepository niveauRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.dev.mot-de-passe}")
    private String motDePasse;

    @Override
    @Transactional
    public void run(String... args) {
        if (utilisateurRepository.existsByEmailIgnoreCase(EMAIL_ELEVE)) {
            return;
        }

        Etablissement ecole = etablissementRepository.findAll().stream()
                .filter(e -> "ECOLE-DEMO-1".equals(e.getCodeOfficiel()))
                .findFirst().orElse(null);
        AnneeScolaire annee = anneeScolaireRepository.findAll().stream()
                .filter(a -> "2026-2027".equals(a.getLibelle()))
                .findFirst().orElse(null);
        Niveau niveau = niveauRepository.findAll().stream()
                .filter(n -> "N1".equals(n.getCode()))
                .findFirst().orElse(null);

        if (ecole == null || annee == null || niveau == null) {
            log.warn("Référentiels de démonstration absents : compte élève non créé. "
                    + "Vérifiez l'ordre des initialiseurs (@Order).");
            return;
        }

        Utilisateur compte = new Utilisateur();
        compte.setEmail(EMAIL_ELEVE);
        compte.setNom("Élève");
        compte.setPrenom("Démo");
        compte.setMotDePasseHash(passwordEncoder.encode(motDePasse));
        utilisateurRepository.save(compte);

        AffectationRole affectation = new AffectationRole();
        affectation.setUtilisateur(compte);
        affectation.setRole(Role.ELEVE);
        affectation.setPerimetre(Perimetre.PERSONNEL);
        affectationRoleRepository.save(affectation);

        Eleve eleve = new Eleve();
        eleve.setMatricule("ELEVE-DEMO-1");
        eleve.setNom("Élève");
        eleve.setPrenom("Démo");
        eleve.setDateNaissance(LocalDate.of(2015, 3, 12));
        eleve.setSexe("F");
        eleve.setUtilisateur(compte);
        eleveRepository.save(eleve);

        Classe classe = new Classe();
        classe.setEtablissement(ecole);
        classe.setAnneeScolaire(annee);
        classe.setNiveau(niveau);
        classe.setNom("6e A");
        classe.setCapacite(30);
        classeRepository.save(classe);

        Inscription inscription = new Inscription();
        inscription.setEleve(eleve);
        inscription.setEtablissement(ecole);
        inscription.setAnneeScolaire(annee);
        inscription.setClasse(classe);
        inscription.setStatut("ACCEPTEE");
        inscription.setDateDemande(LocalDate.of(2026, 9, 1));
        inscription.setDateDecision(LocalDate.of(2026, 9, 2));
        inscriptionRepository.save(inscription);

        log.info("Compte élève de démonstration créé : {} (classe {})", EMAIL_ELEVE, classe.getNom());
    }
}
