package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Niveau;
import com.plateforme.scolaire.domain.ProgrammeMatiere;
import com.plateforme.scolaire.domain.SerieLycee;
import com.plateforme.scolaire.domain.TypeEtablissement;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.repository.NiveauRepository;
import com.plateforme.scolaire.repository.ProgrammeMatiereRepository;
import com.plateforme.scolaire.repository.TypeEtablissementRepository;
import com.plateforme.scolaire.web.dto.CoefficientMatiereDto;
import com.plateforme.scolaire.web.dto.CoefficientsRequest;
import com.plateforme.scolaire.web.dto.CopieProgrammeRequest;
import com.plateforme.scolaire.web.dto.ProgrammeMatiereDto;
import com.plateforme.scolaire.web.dto.ProgrammeMatiereRequest;
import com.plateforme.scolaire.web.dto.ProgrammeResultatDto;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Programme officiel par année scolaire : matières, horaires hebdomadaires et coefficients d'un niveau (et d'une série).
 * Les données sont modifiables par le ministère ; la grille du COC 2023 n'est qu'un point de départ importable.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProgrammeMatiereService {

    static final String RESSOURCE_COC = "referentiel/coc-2023.csv";

    private final ProgrammeMatiereRepository programmes;
    private final TypeEtablissementRepository types;
    private final AnneeScolaireRepository annees;
    private final NiveauRepository niveaux;
    private final MatiereRepository matieres;
    private final ClasseRepository classes;
    private final CoefficientMatiereService coefficients;
    private final AuditService audit;
    private final Clock clock;

    /** Ligne lue dans le fichier du COC. */
    record LigneCoc(List<Integer> ordres, String serie, String code, String libelle, BigDecimal heures, BigDecimal coefficient, String note) {}

    /** Pur : lit le contenu du fichier du COC (champs séparés par « ; », lignes « # » ignorées). */
    static List<LigneCoc> lireCoc(List<String> lignes) {
        List<LigneCoc> resultat = new ArrayList<>();
        for (String l : lignes) {
            if (l.isBlank() || l.startsWith("#")) {
                continue;
            }
            String[] c = l.split(";", -1);
            if (c.length < 7) {
                throw new IllegalArgumentException("Ligne du COC invalide : " + l);
            }
            List<Integer> ordres = new ArrayList<>();
            for (String o : c[0].split("\\|")) {
                ordres.add(Integer.parseInt(o.trim()));
            }
            resultat.add(new LigneCoc(ordres, SerieLycee.normaliser(c[1]), c[2].trim(), c[3].trim(),
                    c[4].isBlank() ? null : new BigDecimal(c[4].trim()), c[5].isBlank() ? null : new BigDecimal(c[5].trim()),
                    c[6].isBlank() ? null : c[6].trim()));
        }
        return resultat;
    }

    /** Pur : coefficients à appliquer à une classe ; la ligne la plus précise (type d'établissement, série) l'emporte sur la ligne commune du niveau. */
    static Map<Long, BigDecimal> coefficientsResolus(List<ProgrammeMatiere> lignes) {
        // Les lignes sans coefficient sont écartées avant le choix : une ligne précise sans coefficient ne masque pas une ligne commune qui en a un
        List<ProgrammeMatiere> retenues = ProgrammeApplicable.retenir(lignes.stream().filter(p -> p.getCoefficient() != null).toList());
        Map<Long, BigDecimal> resultat = new LinkedHashMap<>();
        retenues.forEach(v -> resultat.put(v.getMatiere().getId(), v.getCoefficient()));
        return resultat;
    }

    /** Pur : total d'heures hebdomadaires d'une liste de lignes. */
    public static BigDecimal totalHeures(List<ProgrammeMatiere> lignes) {
        return lignes.stream().map(ProgrammeMatiere::getHeuresHebdo).filter(h -> h != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String cle(Long niveauId, String serie, Long typeId, Long matiereId) {
        return niveauId + "|" + (serie == null ? "" : serie) + "|" + (typeId == null ? 0 : typeId) + "|" + matiereId;
    }

    private static Long typeId(ProgrammeMatiere p) {
        return p.getTypeEtablissement() == null ? null : p.getTypeEtablissement().getId();
    }

    @Transactional(readOnly = true)
    public List<ProgrammeMatiereDto> lister(Long anneeScolaireId, Long niveauId) {
        return (niveauId == null ? programmes.deLAnnee(anneeScolaireId) : programmes.deLAnneeEtNiveau(anneeScolaireId, niveauId)).stream().map(ProgrammeMatiereDto::de).toList();
    }

    public ProgrammeMatiereDto creer(ProgrammeMatiereRequest r) {
        if (r.anneeScolaireId() == null || r.niveauId() == null || r.matiereId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année, niveau et matière sont obligatoires");
        }
        AnneeScolaire annee = annees.findById(r.anneeScolaireId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année inconnue"));
        Niveau niveau = niveaux.findById(r.niveauId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niveau inconnu"));
        Matiere matiere = matieres.findById(r.matiereId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matière inconnue"));
        String serie = normaliserSerie(r.serie());
        TypeEtablissement type = r.typeEtablissementId() == null ? null : types.findById(r.typeEtablissementId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type d'établissement inconnu"));
        Long typeId = type == null ? null : type.getId();
        boolean existe = programmes.deLAnneeEtNiveau(annee.getId(), niveau.getId()).stream()
                .anyMatch(p -> p.getMatiere().getId().equals(matiere.getId()) && java.util.Objects.equals(p.getSerie(), serie)
                        && java.util.Objects.equals(typeId(p), typeId));
        if (existe) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette matière figure déjà au programme de ce niveau pour cette série et ce type d'établissement");
        }
        ProgrammeMatiere p = new ProgrammeMatiere();
        p.setAnneeScolaire(annee);
        p.setNiveau(niveau);
        p.setMatiere(matiere);
        p.setSerie(serie);
        p.setTypeEtablissement(type);
        remplir(p, r);
        programmes.save(p);
        audit.enregistrer("PROGRAMME_LIGNE_CREEE", "ProgrammeMatiere", p.getId(), Map.of("niveau", niveau.getLibelle(), "matiere", matiere.getLibelle()));
        return ProgrammeMatiereDto.de(p);
    }

    public ProgrammeMatiereDto modifier(Long id, ProgrammeMatiereRequest r) {
        ProgrammeMatiere p = programmes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ligne introuvable"));
        remplir(p, r);
        audit.enregistrer("PROGRAMME_LIGNE_MODIFIEE", "ProgrammeMatiere", id, Map.of("matiere", p.getMatiere().getLibelle()));
        return ProgrammeMatiereDto.de(p);
    }

    public void supprimer(Long id) {
        ProgrammeMatiere p = programmes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ligne introuvable"));
        audit.enregistrer("PROGRAMME_LIGNE_SUPPRIMEE", "ProgrammeMatiere", id, Map.of("matiere", p.getMatiere().getLibelle()));
        programmes.delete(p);
    }

    private void remplir(ProgrammeMatiere p, ProgrammeMatiereRequest r) {
        p.setHeuresHebdo(r.heuresHebdo());
        p.setCoefficient(r.coefficient());
        p.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        p.setUpdatedAt(Instant.now(clock));
    }

    private static String normaliserSerie(String code) {
        try {
            return SerieLycee.normaliser(code);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** Importe la grille du COC 2023 dans l'année : crée les matières manquantes, n'écrase aucune ligne existante. */
    public ProgrammeResultatDto importerCoc(Long anneeScolaireId) {
        AnneeScolaire annee = annees.findById(anneeScolaireId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année inconnue"));
        List<LigneCoc> lignes;
        try (BufferedReader lecteur = new BufferedReader(new InputStreamReader(new ClassPathResource(RESSOURCE_COC).getInputStream(), StandardCharsets.UTF_8))) {
            lignes = lireCoc(lecteur.lines().toList());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Grille du COC introuvable");
        }
        Map<Integer, Niveau> parOrdre = new HashMap<>();
        niveaux.findAllByOrderByOrdre().forEach(n -> parOrdre.putIfAbsent(n.getOrdre(), n));
        Map<String, Matiere> parCode = new HashMap<>();
        matieres.findAll().forEach(m -> parCode.put(m.getCode(), m));
        Set<String> existantes = new HashSet<>();
        programmes.deLAnnee(anneeScolaireId).forEach(p -> existantes.add(cle(p.getNiveau().getId(), p.getSerie(), typeId(p), p.getMatiere().getId())));

        int creees = 0;
        int ignorees = 0;
        int matieresCreees = 0;
        Set<String> avertissements = new java.util.LinkedHashSet<>();
        for (LigneCoc l : lignes) {
            Matiere matiere = parCode.get(l.code());
            if (matiere == null) {
                matiere = new Matiere();
                matiere.setCode(l.code());
                matiere.setLibelle(l.libelle());
                matiere = matieres.save(matiere);
                parCode.put(l.code(), matiere);
                matieresCreees++;
            }
            for (int ordre : l.ordres()) {
                Niveau niveau = parOrdre.get(ordre);
                if (niveau == null) {
                    avertissements.add("Aucun niveau d'ordre " + ordre + " : lignes ignorées");
                    ignorees++;
                    continue;
                }
                if (!existantes.add(cle(niveau.getId(), l.serie(), null, matiere.getId()))) {
                    ignorees++;
                    continue;
                }
                ProgrammeMatiere p = new ProgrammeMatiere();
                p.setAnneeScolaire(annee);
                p.setNiveau(niveau);
                p.setSerie(l.serie());
                p.setMatiere(matiere);
                p.setHeuresHebdo(l.heures());
                p.setCoefficient(l.coefficient());
                p.setNote(l.note());
                p.setUpdatedAt(Instant.now(clock));
                programmes.save(p);
                creees++;
            }
        }
        audit.enregistrer("PROGRAMME_IMPORT_COC", "AnneeScolaire", anneeScolaireId, Map.of("creees", creees, "ignorees", ignorees));
        return new ProgrammeResultatDto(creees, ignorees, matieresCreees, List.copyOf(avertissements));
    }

    /** Copie le programme d'une année vers une autre (ne remplace pas les lignes déjà présentes). */
    public ProgrammeResultatDto copier(CopieProgrammeRequest r) {
        if (r.deAnneeScolaireId().equals(r.versAnneeScolaireId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les deux années doivent être différentes");
        }
        AnneeScolaire vers = annees.findById(r.versAnneeScolaireId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année de destination inconnue"));
        Set<String> existantes = new HashSet<>();
        programmes.deLAnnee(vers.getId()).forEach(p -> existantes.add(cle(p.getNiveau().getId(), p.getSerie(), typeId(p), p.getMatiere().getId())));
        int creees = 0;
        int ignorees = 0;
        for (ProgrammeMatiere source : programmes.deLAnnee(r.deAnneeScolaireId())) {
            if (!existantes.add(cle(source.getNiveau().getId(), source.getSerie(), typeId(source), source.getMatiere().getId()))) {
                ignorees++;
                continue;
            }
            ProgrammeMatiere p = new ProgrammeMatiere();
            p.setAnneeScolaire(vers);
            p.setNiveau(source.getNiveau());
            p.setSerie(source.getSerie());
            p.setTypeEtablissement(source.getTypeEtablissement());
            p.setMatiere(source.getMatiere());
            p.setHeuresHebdo(source.getHeuresHebdo());
            p.setCoefficient(source.getCoefficient());
            p.setNote(source.getNote());
            p.setUpdatedAt(Instant.now(clock));
            programmes.save(p);
            creees++;
        }
        audit.enregistrer("PROGRAMME_COPIE", "AnneeScolaire", vers.getId(), Map.of("depuis", r.deAnneeScolaireId(), "creees", creees));
        return new ProgrammeResultatDto(creees, ignorees, 0, List.of());
    }

    /** Applique les coefficients du programme (niveau et série de la classe, année de la classe) aux coefficients de la classe. */
    public List<CoefficientMatiereDto> appliquerAClasse(Long classeId) {
        Classe classe = classes.findById(classeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
        List<ProgrammeMatiere> lignes = ProgrammeApplicable.pourClasse(programmes, classe);
        Map<Long, BigDecimal> coefs = coefficientsResolus(lignes);
        if (coefs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun coefficient au programme de ce niveau pour cette année");
        }
        List<CoefficientsRequest.Ligne> demandes = coefs.entrySet().stream().map(e -> new CoefficientsRequest.Ligne(e.getKey(), e.getValue())).toList();
        return coefficients.enregistrer(classeId, new CoefficientsRequest(demandes));
    }
}
