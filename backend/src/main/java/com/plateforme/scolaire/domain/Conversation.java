package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fil de discussion entre un parent et un membre du personnel, à propos d'un élève. */
@Entity
@Table(name = "conversation")
@Getter
@Setter
@NoArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id")
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id")
    private Utilisateur parent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "personnel_id")
    private Utilisateur personnel;

    @Column(nullable = false, length = 200)
    private String sujet;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "dernier_message_le", nullable = false)
    private Instant dernierMessageLe;

    @Column(name = "dernier_auteur_id", nullable = false)
    private Long dernierAuteurId;

    @Column(name = "dernier_extrait", nullable = false, length = 140)
    private String dernierExtrait;

    @Column(name = "parent_lu_le")
    private Instant parentLuLe;

    @Column(name = "personnel_lu_le")
    private Instant personnelLuLe;

    public boolean estParticipant(Long utilisateurId) {
        return parent.getId().equals(utilisateurId) || personnel.getId().equals(utilisateurId);
    }

    /** Le dernier message vient de l'autre personne et n'a pas encore été lu par [utilisateurId]. */
    public boolean nonLuPar(Long utilisateurId) {
        if (dernierAuteurId.equals(utilisateurId)) {
            return false;
        }
        Instant lu = parent.getId().equals(utilisateurId) ? parentLuLe : personnelLuLe;
        return lu == null || lu.isBefore(dernierMessageLe);
    }

    public void marquerLu(Long utilisateurId, Instant quand) {
        if (parent.getId().equals(utilisateurId)) {
            parentLuLe = quand;
        } else {
            personnelLuLe = quand;
        }
    }
}
