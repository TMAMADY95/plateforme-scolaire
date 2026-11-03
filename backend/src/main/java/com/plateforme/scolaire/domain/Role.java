package com.plateforme.scolaire.domain;

public enum Role {
    ADMIN_TECHNIQUE,
    /** Ministère : fixe le calendrier, proclame les résultats, consulte tout le reste. */
    MINISTERE,
    /** Direction générale des examens et concours scolaires : pilote technique des examens, au niveau national. */
    DGECS,
    /** Inspection régionale de l'éducation (IRE) : supervise les examens de sa région. */
    DIRECTION_REGIONALE,
    /** Direction préfectorale (DPE) ou communale (DCE) de l'éducation : un seul rôle, distingué par la zone. */
    DIRECTION_LOCALE,
    /** Chef d'un centre d'examen : saisit le personnel, les incidents et les rapports de son centre. */
    CHEF_CENTRE,
    DIRECTION_ETABLISSEMENT,
    PERSONNEL_ADMIN,
    ENSEIGNANT,
    PARENT,
    ELEVE
}
