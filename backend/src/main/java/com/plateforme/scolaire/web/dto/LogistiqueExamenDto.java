package com.plateforme.scolaire.web.dto;

import java.util.List;

/**
 * Préparation matérielle et humaine des centres d'un examen : personnel requis et pourvu, fournitures livrées, centres prêts.
 * Les taux sont en pourcentage au dixième.
 */
public record LogistiqueExamenDto(Long examenId, Resume resume, List<LigneCentre> centres) {

    public record Resume(int centres, int personnelComplets, int approvisionnes, int prets, int enRetard, int postesRequis, int postesPourvus,
            Double tauxPostesPourvus, Double tauxApprovisionnes, Double tauxPrets) {
    }

    public record Livraison(String article, String statut, int quantitePrevue, Integer quantiteLivree, Integer ecart, String observation) {
    }

    public record LigneCentre(Long centreId, String nom, String zone, int salles, int postesRequis, int postesPourvus, boolean personnelComplet,
            List<Livraison> livraisons, boolean approvisionne, boolean pret, boolean enRetard) {
    }
}
