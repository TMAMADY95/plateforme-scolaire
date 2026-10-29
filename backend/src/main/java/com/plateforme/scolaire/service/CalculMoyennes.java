package com.plateforme.scolaire.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Moyennes pondérées sur 20.
 * <ul>
 *   <li>Une note est ramenée sur 20 (valeur / barème x 20), puis pondérée par le coefficient de l'évaluation.</li>
 *   <li>Moyenne d'une matière = moyenne pondérée de ses notes.</li>
 *   <li>Moyenne générale = moyenne des moyennes de matières, pondérée par le coefficient de chaque matière dans la
 *   classe (1 par défaut).</li>
 *   <li>Rang : classement "à égalité" sur les moyennes arrondies à 2 décimales (1, 2, 2, 4...).</li>
 * </ul>
 * Les valeurs renvoyées ont 6 décimales : arrondir à l'affichage avec {@link #arrondir(BigDecimal)}.
 */
public final class CalculMoyennes {

    private static final BigDecimal VINGT = BigDecimal.valueOf(20);

    /** Une note chiffrée (les absences et notes manquantes ne sont pas fournies). */
    public record NoteCalcul(Long inscriptionId, Long matiereId, BigDecimal valeur, BigDecimal bareme,
                             BigDecimal coefficient) {
    }

    public record Resultat(Map<Long, Map<Long, BigDecimal>> moyennesParEleve,
                           Map<Long, BigDecimal> moyennesGenerales,
                           Map<Long, Integer> rangs,
                           Map<Long, BigDecimal> moyennesClasseParMatiere,
                           BigDecimal moyenneGeneraleClasse) {
    }

    private CalculMoyennes() {
    }

    public static BigDecimal arrondir(BigDecimal valeur) {
        return valeur == null ? null : valeur.setScale(2, RoundingMode.HALF_UP);
    }

    public static Resultat calculer(Collection<NoteCalcul> notes) {
        return calculer(notes, Map.of());
    }

    /**
     * [coefficientsMatieres] : poids de chaque matière dans la moyenne générale ; une matière absente de la table
     * pèse 1. Les moyennes de matières, elles, ne dépendent pas de ces coefficients.
     */
    public static Resultat calculer(Collection<NoteCalcul> notes, Map<Long, BigDecimal> coefficientsMatieres) {
        // inscription -> matière -> [somme pondérée, somme des coefficients]
        Map<Long, Map<Long, BigDecimal[]>> cumuls = new LinkedHashMap<>();
        for (NoteCalcul n : notes) {
            BigDecimal sur20 = n.valeur().multiply(VINGT).divide(n.bareme(), 8, RoundingMode.HALF_UP);
            BigDecimal[] cumul = cumuls
                    .computeIfAbsent(n.inscriptionId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(n.matiereId(), k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            cumul[0] = cumul[0].add(sur20.multiply(n.coefficient()));
            cumul[1] = cumul[1].add(n.coefficient());
        }

        Map<Long, Map<Long, BigDecimal>> moyennesParEleve = new LinkedHashMap<>();
        Map<Long, BigDecimal> moyennesGenerales = new LinkedHashMap<>();
        cumuls.forEach((inscriptionId, matieres) -> {
            Map<Long, BigDecimal> moyennes = new LinkedHashMap<>();
            matieres.forEach((matiereId, c) -> moyennes.put(matiereId, c[0].divide(c[1], 6, RoundingMode.HALF_UP)));
            moyennesParEleve.put(inscriptionId, moyennes);
            moyennesGenerales.put(inscriptionId, moyennePonderee(moyennes, coefficientsMatieres));
        });

        Map<Long, List<BigDecimal>> valeursParMatiere = new LinkedHashMap<>();
        moyennesParEleve.values().forEach(moyennes -> moyennes.forEach((matiereId, valeur) ->
                valeursParMatiere.computeIfAbsent(matiereId, k -> new ArrayList<>()).add(valeur)));
        Map<Long, BigDecimal> moyennesClasse = new LinkedHashMap<>();
        valeursParMatiere.forEach((matiereId, valeurs) -> moyennesClasse.put(matiereId, moyenne(valeurs)));

        return new Resultat(moyennesParEleve, moyennesGenerales, rangs(moyennesGenerales), moyennesClasse,
                moyennesGenerales.isEmpty() ? null : moyenne(moyennesGenerales.values()));
    }

    /** Seules les matières où l'élève a une note comptent, chacune selon son coefficient. */
    private static BigDecimal moyennePonderee(Map<Long, BigDecimal> moyennes, Map<Long, BigDecimal> coefficients) {
        BigDecimal somme = BigDecimal.ZERO;
        BigDecimal poids = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> e : moyennes.entrySet()) {
            BigDecimal coefficient = coefficients.getOrDefault(e.getKey(), BigDecimal.ONE);
            somme = somme.add(e.getValue().multiply(coefficient));
            poids = poids.add(coefficient);
        }
        return somme.divide(poids, 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal moyenne(Collection<BigDecimal> valeurs) {
        BigDecimal somme = valeurs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return somme.divide(BigDecimal.valueOf(valeurs.size()), 6, RoundingMode.HALF_UP);
    }

    private static Map<Long, Integer> rangs(Map<Long, BigDecimal> moyennesGenerales) {
        Map<Long, Integer> rangs = new LinkedHashMap<>();
        moyennesGenerales.forEach((inscriptionId, moyenne) -> {
            long meilleurs = moyennesGenerales.values().stream()
                    .filter(autre -> arrondir(autre).compareTo(arrondir(moyenne)) > 0)
                    .count();
            rangs.put(inscriptionId, (int) meilleurs + 1);
        });
        return rangs;
    }
}
