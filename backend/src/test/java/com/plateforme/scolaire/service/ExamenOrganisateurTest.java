package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ZoneGeographique;
import org.junit.jupiter.api.Test;

class ExamenOrganisateurTest {

    private static ZoneGeographique zone(String nom, String type, ZoneGeographique parent) {
        ZoneGeographique z = new ZoneGeographique();
        z.setNom(nom);
        z.setTypeZone(type);
        z.setParent(parent);
        return z;
    }

    private static Etablissement ecoleDans(ZoneGeographique z) {
        Etablissement e = new Etablissement();
        e.setZone(z);
        return e;
    }

    @Test
    void laZoneOrganisatriceEstLAncetreDuTypeDemande() {
        ZoneGeographique region = zone("Kindia", "REGION", null);
        ZoneGeographique prefecture = zone("Coyah", "PREFECTURE", region);
        ZoneGeographique commune = zone("Coyah-centre", "COMMUNE", prefecture);
        Etablissement ecole = ecoleDans(commune);

        assertThat(ExamenService.organisateur(ecole, "COMMUNE")).isEqualTo("Coyah-centre");
        assertThat(ExamenService.organisateur(ecole, "PREFECTURE")).isEqualTo("Coyah");
        assertThat(ExamenService.organisateur(ecole, "region")).isEqualTo("Kindia");
    }

    @Test
    void unExamenNationalOuUneZoneAbsenteDonnentAucunOrganisateurLocal() {
        Etablissement ecole = ecoleDans(zone("Coyah", "PREFECTURE", null));

        assertThat(ExamenService.organisateur(ecole, "NATIONAL")).isNull();
        assertThat(ExamenService.organisateur(ecole, "REGION")).isNull();
        assertThat(ExamenService.organisateur(ecoleDans(null), "COMMUNE")).isNull();
    }

    private static java.util.List<com.plateforme.scolaire.security.UtilisateurPrincipal.AffectationInfo> compte(
            com.plateforme.scolaire.domain.Role role, Long zoneId) {
        return java.util.List.of(new com.plateforme.scolaire.security.UtilisateurPrincipal.AffectationInfo(
                role, com.plateforme.scolaire.domain.Perimetre.ZONE, zoneId, null));
    }

    @Test
    void lesResultatsSontSaisisParLaZoneOrganisatriceOuAuDessus() {
        ZoneGeographique region = zone("Kindia", "REGION", null);
        region.setId(1L);
        ZoneGeographique prefecture = zone("Coyah", "PREFECTURE", region);
        prefecture.setId(2L);
        ZoneGeographique commune = zone("Coyah-centre", "COMMUNE", prefecture);
        commune.setId(3L);
        Etablissement ecole = ecoleDans(commune);
        var role = com.plateforme.scolaire.domain.Role.DIRECTION_REGIONALE;

        // examen organisé par la préfecture : la préfecture et la région saisissent, la commune non
        assertThat(ExamenService.peutSaisir(ecole, "PREFECTURE", compte(role, 2L))).isTrue();
        assertThat(ExamenService.peutSaisir(ecole, "PREFECTURE", compte(role, 1L))).isTrue();
        assertThat(ExamenService.peutSaisir(ecole, "PREFECTURE", compte(role, 3L))).isFalse();
        // examen régional : un compte communal ne saisit pas
        assertThat(ExamenService.peutSaisir(ecole, "REGION", compte(role, 3L))).isFalse();
        assertThat(ExamenService.peutSaisir(ecole, "REGION", compte(role, 1L))).isTrue();
        // le ministère saisit tout ; un examen national dépend seulement du périmètre
        assertThat(ExamenService.peutSaisir(ecole, "REGION", compte(com.plateforme.scolaire.domain.Role.MINISTERE, null))).isTrue();
        assertThat(ExamenService.peutSaisir(ecole, "NATIONAL", compte(role, 3L))).isTrue();
    }
}
