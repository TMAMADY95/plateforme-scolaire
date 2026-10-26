package com.plateforme.scolaire.dev;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.repository.ZoneGeographiqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Crée des comptes et des établissements de démonstration. Actif uniquement avec le profil "dev". */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements CommandLineRunner {

    private static final String EMAIL_MINISTERE = "ministere@example.org";
    private static final String EMAIL_DIRECTION = "direction1@example.org";

    private final UtilisateurRepository utilisateurRepository;
    private final AffectationRoleRepository affectationRoleRepository;
    private final EtablissementRepository etablissementRepository;
    private final ZoneGeographiqueRepository zoneRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.dev.mot-de-passe}")
    private String motDePasse;

    @Override
    @Transactional
    public void run(String... args) {
        if (utilisateurRepository.existsByEmailIgnoreCase(EMAIL_MINISTERE)) {
            return;
        }

        ZoneGeographique pays = new ZoneGeographique();
        pays.setNom("Pays démo");
        pays.setTypeZone("PAYS");
        zoneRepository.save(pays);

        Etablissement ecole1 = etablissement("ECOLE-DEMO-1", "École démo 1", pays);
        etablissement("ECOLE-DEMO-2", "École démo 2", pays);

        Utilisateur ministere = utilisateur(EMAIL_MINISTERE, "Ministère", "Démo");
        affecter(ministere, Role.MINISTERE, Perimetre.NATIONAL, null);

        Utilisateur direction = utilisateur(EMAIL_DIRECTION, "Direction", "École 1");
        affecter(direction, Role.DIRECTION_ETABLISSEMENT, Perimetre.ETABLISSEMENT, ecole1);

        log.info("Comptes de démonstration créés : {} (national) et {} (École démo 1 uniquement)",
                EMAIL_MINISTERE, EMAIL_DIRECTION);
    }

    private Etablissement etablissement(String code, String nom, ZoneGeographique zone) {
        Etablissement e = new Etablissement();
        e.setCodeOfficiel(code);
        e.setNom(nom);
        e.setZone(zone);
        e.setTypeEtablissement("PRIMAIRE");
        return etablissementRepository.save(e);
    }

    private Utilisateur utilisateur(String email, String nom, String prenom) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setMotDePasseHash(passwordEncoder.encode(motDePasse));
        return utilisateurRepository.save(u);
    }

    private void affecter(Utilisateur u, Role role, Perimetre perimetre, Etablissement etablissement) {
        AffectationRole a = new AffectationRole();
        a.setUtilisateur(u);
        a.setRole(role);
        a.setPerimetre(perimetre);
        a.setEtablissement(etablissement);
        affectationRoleRepository.save(a);
    }
}
