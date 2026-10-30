package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Annonce;
import java.time.Instant;

/** [modifiable] : l'utilisateur courant peut supprimer cette annonce (son auteur, ou la direction de l'école). */
public record AnnonceDto(Long id, Long classeId, String auteur, String titre, String contenu, Instant publieLe,
                         boolean modifiable) {

    public static AnnonceDto de(Annonce a, boolean modifiable) {
        var u = a.getAuteur();
        return new AnnonceDto(a.getId(), a.getClasse().getId(), (u.getPrenom() + " " + u.getNom()).trim(),
                a.getTitre(), a.getContenu(), a.getCreatedAt(), modifiable);
    }
}
