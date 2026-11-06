package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.FicheInfrastructure;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.FicheInfrastructureRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.InfrastructureDto;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Carte scolaire : infrastructures et équipements de chaque établissement, rapprochés des effectifs inscrits.
 * Les seuils d'alerte sont des hypothèses de travail à confirmer avec le ministère.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InfrastructureService {

    /** Au-delà, la salle est jugée surchargée. */
    static final double SEUIL_ELEVES_PAR_SALLE = 60;
    /** Nombre maximal d'élèves (de chaque sexe) par latrine. */
    static final double SEUIL_ELEVES_PAR_LATRINE = 50;

    private static final Set<String> EAU = Set.of("AUCUNE", "PUITS", "FORAGE", "RESEAU");
    private static final Set<String> ELECTRICITE = Set.of("AUCUNE", "RESEAU", "SOLAIRE", "GROUPE");

    private final FicheInfrastructureRepository fiches;
    private final EtablissementRepository etablissements;
    private final AnneeScolaireRepository annees;
    private final PerimetreService perimetre;
    private final EntityManager em;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : quotient arrondi à 0,1 ; null si le diviseur est nul. */
    static Double ratio(int numerateur, int diviseur) {
        return diviseur <= 0 ? null : Math.round(numerateur * 10.0 / diviseur) / 10.0;
    }

    /** Pur : calcule les ratios et les constats à partir d'une fiche et des effectifs. */
    static InfrastructureDto.Indicateurs indicateurs(FicheInfrastructure f, int total, int filles, int garcons) {
        int salles = f.getSallesDur() + f.getSallesProvisoires();
        Double parSalle = ratio(total, salles);
        Double parPlace = ratio(total, f.getPlacesAssises());
        Double fillesParLatrine = ratio(filles, f.getLatrinesFilles());
        Double garconsParLatrine = ratio(garcons, f.getLatrinesGarcons());
        List<String> alertes = new ArrayList<>();
        if (total > 0 && salles == 0) {
            alertes.add("Aucune salle de classe déclarée");
        } else if (parSalle != null && parSalle > SEUIL_ELEVES_PAR_SALLE) {
            alertes.add("Salles surchargées : " + parSalle + " élèves par salle (seuil " + (int) SEUIL_ELEVES_PAR_SALLE + ")");
        }
        if (total > f.getPlacesAssises()) {
            alertes.add("Places assises insuffisantes : " + f.getPlacesAssises() + " pour " + total + " élèves");
        }
        if (filles > 0 && f.getLatrinesFilles() == 0) {
            alertes.add("Aucune latrine pour les filles");
        } else if (fillesParLatrine != null && fillesParLatrine > SEUIL_ELEVES_PAR_LATRINE) {
            alertes.add("Latrines filles insuffisantes : " + fillesParLatrine + " filles par latrine");
        }
        if (garcons > 0 && f.getLatrinesGarcons() == 0) {
            alertes.add("Aucune latrine pour les garçons");
        } else if (garconsParLatrine != null && garconsParLatrine > SEUIL_ELEVES_PAR_LATRINE) {
            alertes.add("Latrines garçons insuffisantes : " + garconsParLatrine + " garçons par latrine");
        }
        if ("AUCUNE".equals(f.getEau())) {
            alertes.add("Pas de point d'eau");
        }
        if (f.getSallesARehabiliter() > 0) {
            alertes.add(f.getSallesARehabiliter() + " salle(s) à réhabiliter");
        }
        return new InfrastructureDto.Indicateurs(parSalle, parPlace, fillesParLatrine, garconsParLatrine, List.copyOf(alertes));
    }

    private static InfrastructureDto.Requete enRequete(FicheInfrastructure f) {
        return new InfrastructureDto.Requete(f.getSallesDur(), f.getSallesProvisoires(), f.getSallesARehabiliter(), f.getPlacesAssises(),
                f.getLatrinesFilles(), f.getLatrinesGarcons(), f.getLatrinesPersonnel(), f.getEau(), f.getElectricite(), f.isCloture(),
                f.isLavageMains(), f.isBibliotheque(), f.isLaboratoire(), f.isSalleInformatique(), f.isCantine(), f.isTerrainSport(), f.getObservation());
    }

    /** Effectifs ACCEPTEE par établissement : [total, filles, garçons]. */
    @SuppressWarnings("unchecked")
    private Map<Long, int[]> effectifs(Long anneeId, Long etablissementId) {
        String sql = "SELECT i.etablissement_id, e.sexe, count(*) FROM inscription i JOIN eleve e ON e.id = i.eleve_id "
                + "WHERE i.annee_scolaire_id = :a AND i.statut = 'ACCEPTEE' "
                + (etablissementId == null ? "" : "AND i.etablissement_id = :e ")
                + "GROUP BY i.etablissement_id, e.sexe";
        var requete = em.createNativeQuery(sql).setParameter("a", anneeId);
        if (etablissementId != null) {
            requete.setParameter("e", etablissementId);
        }
        List<Object[]> rows = requete.getResultList();
        Map<Long, int[]> res = new HashMap<>();
        for (Object[] r : rows) {
            int[] t = res.computeIfAbsent(((Number) r[0]).longValue(), k -> new int[3]);
            int n = ((Number) r[2]).intValue();
            t[0] += n;
            if ("F".equals(r[1])) {
                t[1] += n;
            } else if ("M".equals(r[1])) {
                t[2] += n;
            }
        }
        return res;
    }

    private void verifierAnnee(Long anneeId) {
        if (!annees.existsById(anneeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Année scolaire introuvable");
        }
    }

    @Transactional(readOnly = true)
    public InfrastructureDto fiche(Long etablissementId, Long anneeId) {
        if (!etablissements.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        verifierAnnee(anneeId);
        int[] eff = effectifs(anneeId, etablissementId).getOrDefault(etablissementId, new int[3]);
        FicheInfrastructure f = fiches.findByEtablissementIdAndAnneeScolaireId(etablissementId, anneeId).orElse(null);
        boolean renseignee = f != null;
        FicheInfrastructure base = renseignee ? f : new FicheInfrastructure();
        return new InfrastructureDto(etablissementId, anneeId, renseignee, new InfrastructureDto.Effectifs(eff[0], eff[1], eff[2]), enRequete(base),
                indicateurs(base, eff[0], eff[1], eff[2]), renseignee ? f.getMisAJourLe() : null, renseignee ? f.getMisAJourPar() : null);
    }

    public InfrastructureDto enregistrer(Long etablissementId, Long anneeId, InfrastructureDto.Requete q, String auteur) {
        if (!etablissements.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        verifierAnnee(anneeId);
        if (!EAU.contains(q.eau())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source d'eau inconnue : " + q.eau());
        }
        if (!ELECTRICITE.contains(q.electricite())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source d'électricité inconnue : " + q.electricite());
        }
        if (q.sallesARehabiliter() > q.sallesDur() + q.sallesProvisoires()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les salles à réhabiliter ne peuvent pas dépasser le nombre de salles");
        }
        FicheInfrastructure f = fiches.findByEtablissementIdAndAnneeScolaireId(etablissementId, anneeId).orElseGet(() -> {
            FicheInfrastructure n = new FicheInfrastructure();
            n.setEtablissementId(etablissementId);
            n.setAnneeScolaireId(anneeId);
            return n;
        });
        f.setSallesDur(q.sallesDur());
        f.setSallesProvisoires(q.sallesProvisoires());
        f.setSallesARehabiliter(q.sallesARehabiliter());
        f.setPlacesAssises(q.placesAssises());
        f.setLatrinesFilles(q.latrinesFilles());
        f.setLatrinesGarcons(q.latrinesGarcons());
        f.setLatrinesPersonnel(q.latrinesPersonnel());
        f.setEau(q.eau());
        f.setElectricite(q.electricite());
        f.setCloture(q.cloture());
        f.setLavageMains(q.lavageMains());
        f.setBibliotheque(q.bibliotheque());
        f.setLaboratoire(q.laboratoire());
        f.setSalleInformatique(q.salleInformatique());
        f.setCantine(q.cantine());
        f.setTerrainSport(q.terrainSport());
        f.setObservation(q.observation() == null || q.observation().isBlank() ? null : q.observation().trim());
        f.setMisAJourLe(Instant.now(clock));
        f.setMisAJourPar(auteur);
        fiches.save(f);
        audit.enregistrer("INFRASTRUCTURE_ENREGISTREE", "Etablissement", etablissementId, Map.of("annee", anneeId, "salles", q.sallesDur() + q.sallesProvisoires()));
        return fiche(etablissementId, anneeId);
    }

    @Transactional(readOnly = true)
    public InfrastructureDto.Synthese synthese(Long anneeId) {
        verifierAnnee(anneeId);
        UtilisateurPrincipal p = perimetre.utilisateurCourant();
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        Map<Long, int[]> eff = effectifs(anneeId, null);
        Map<Long, FicheInfrastructure> parEtab = new HashMap<>();
        fiches.findByAnneeScolaireId(anneeId).forEach(f -> parEtab.put(f.getEtablissementId(), f));
        List<InfrastructureDto.Ligne> lignes = new ArrayList<>();
        int renseignes = 0;
        int avecAlertes = 0;
        int salles = 0;
        int provisoires = 0;
        int rehab = 0;
        int eleves = 0;
        int sallesDesRenseignes = 0;
        int elevesDesRenseignes = 0;
        int sansEau = 0;
        int sansElec = 0;
        int sansLatrinesFilles = 0;
        for (Etablissement e : perimetre.etablissementsAccessibles(p)) {
            int[] t = eff.getOrDefault(e.getId(), new int[3]);
            FicheInfrastructure f = parEtab.get(e.getId());
            String zone = e.getZone() == null ? null : e.getZone().getNom();
            eleves += t[0];
            if (f == null) {
                lignes.add(new InfrastructureDto.Ligne(e.getId(), e.getNom(), zone, t[0], false, 0, null, null, null, null, List.of()));
                continue;
            }
            renseignes++;
            InfrastructureDto.Indicateurs ind = indicateurs(f, t[0], t[1], t[2]);
            int s = f.getSallesDur() + f.getSallesProvisoires();
            salles += s;
            provisoires += f.getSallesProvisoires();
            rehab += f.getSallesARehabiliter();
            sallesDesRenseignes += s;
            elevesDesRenseignes += t[0];
            if (!ind.alertes().isEmpty()) {
                avecAlertes++;
            }
            if ("AUCUNE".equals(f.getEau())) {
                sansEau++;
            }
            if ("AUCUNE".equals(f.getElectricite())) {
                sansElec++;
            }
            if (f.getLatrinesFilles() == 0) {
                sansLatrinesFilles++;
            }
            lignes.add(new InfrastructureDto.Ligne(e.getId(), e.getNom(), zone, t[0], true, s, ind.elevesParSalle(), ind.elevesParPlace(), f.getEau(),
                    f.getElectricite(), ind.alertes()));
        }
        lignes.sort(Comparator.comparing((InfrastructureDto.Ligne l) -> l.alertes().size()).reversed().thenComparing(InfrastructureDto.Ligne::etablissement));
        return new InfrastructureDto.Synthese(anneeId, new InfrastructureDto.Resume(lignes.size(), renseignes, avecAlertes, salles, provisoires, rehab,
                ratio(elevesDesRenseignes, sallesDesRenseignes), sansEau, sansElec, sansLatrinesFilles), lignes);
    }
}
