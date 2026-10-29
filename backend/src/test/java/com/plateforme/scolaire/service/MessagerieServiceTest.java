package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AffectationEnseignant;
import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Conversation;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Enseignant;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.LienParental;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Message;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationEnseignantRepository;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.ConversationRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.repository.MessageRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.ContactDto;
import com.plateforme.scolaire.web.dto.ConversationDto;
import com.plateforme.scolaire.web.dto.NouvelleConversationRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

class MessagerieServiceTest {

    private static final long ELEVE = 1L;
    private static final long PARENT = 10L;
    private static final long PROF = 20L;
    private static final long DIRECTION = 30L;
    private static final long ETRANGER = 99L;
    private static final long CLASSE = 5L;
    private static final long ETAB = 8L;

    private final ConversationRepository conversations = mock(ConversationRepository.class);
    private final MessageRepository messages = mock(MessageRepository.class);
    private final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    private final EleveRepository eleves = mock(EleveRepository.class);
    private final LienParentalRepository liens = mock(LienParentalRepository.class);
    private final InscriptionRepository inscriptions = mock(InscriptionRepository.class);
    private final AffectationEnseignantRepository affectations = mock(AffectationEnseignantRepository.class);
    private final AffectationRoleRepository roles = mock(AffectationRoleRepository.class);
    private final PerimetreService perimetre = mock(PerimetreService.class);
    private final Clock horloge = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);
    private final EnvoyeurMail mail = mock(EnvoyeurMail.class);
    private final MessagerieService service = new MessagerieService(conversations, messages, utilisateurs, eleves,
            liens, inscriptions, affectations, roles, perimetre, horloge, mail);

    private Eleve eleve;
    private final Utilisateur parent = utilisateur(PARENT, "Awa", "Diallo");
    private final Utilisateur prof = utilisateur(PROF, "Moussa", "Ba");
    private final Utilisateur direction = utilisateur(DIRECTION, "Fatou", "Sow");

    private static Utilisateur utilisateur(long id, String prenom, String nom) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setPrenom(prenom);
        u.setNom(nom);
        u.setEmail(prenom.toLowerCase() + "@example.org");
        return u;
    }

    private void connecteEn(long id) {
        UtilisateurPrincipal p = mock(UtilisateurPrincipal.class);
        when(p.getId()).thenReturn(id);
        when(perimetre.utilisateurCourant()).thenReturn(p);
    }

    @BeforeEach
    void preparer() {
        eleve = new Eleve();
        eleve.setId(ELEVE);
        eleve.setPrenom("Fanta");
        eleve.setNom("Diallo");
        when(eleves.findById(ELEVE)).thenReturn(Optional.of(eleve));

        Etablissement etab = new Etablissement();
        etab.setId(ETAB);
        Classe classe = new Classe();
        classe.setId(CLASSE);
        Inscription inscription = new Inscription();
        inscription.setEleve(eleve);
        inscription.setEtablissement(etab);
        inscription.setClasse(classe);
        when(inscriptions.findByEleveIdAndStatutOrderByAnneeScolaireDateDebutDesc(ELEVE, "ACCEPTEE"))
                .thenReturn(List.of(inscription));

        when(liens.existsByEleveIdAndUtilisateurId(ELEVE, PARENT)).thenReturn(true);
        LienParental lien = new LienParental();
        lien.setEleve(eleve);
        lien.setUtilisateur(parent);
        lien.setRelation("Mère");
        when(liens.findByEleveId(ELEVE)).thenReturn(List.of(lien));

        when(affectations.existsByClasseIdAndEnseignantUtilisateurId(CLASSE, PROF)).thenReturn(true);
        Enseignant enseignant = new Enseignant();
        enseignant.setUtilisateur(prof);
        Matiere maths = new Matiere();
        maths.setLibelle("Mathématiques");
        Matiere physique = new Matiere();
        physique.setLibelle("Physique");
        AffectationEnseignant a1 = new AffectationEnseignant();
        a1.setEnseignant(enseignant);
        a1.setMatiere(maths);
        AffectationEnseignant a2 = new AffectationEnseignant();
        a2.setEnseignant(enseignant);
        a2.setMatiere(physique);
        when(affectations.findByClasseId(CLASSE)).thenReturn(List.of(a1, a2));

        AffectationRole roleDirection = new AffectationRole();
        roleDirection.setUtilisateur(direction);
        roleDirection.setRole(Role.DIRECTION_ETABLISSEMENT);
        roleDirection.setEtablissement(etab);
        when(roles.findByUtilisateurId(DIRECTION)).thenReturn(List.of(roleDirection));
        when(roles.rechercherComptes(org.mockito.ArgumentMatchers.eq(ETAB), anyCollection(), anyString()))
                .thenReturn(List.of(roleDirection));

        when(utilisateurs.getReferenceById(PARENT)).thenReturn(parent);
        when(utilisateurs.getReferenceById(PROF)).thenReturn(prof);
        when(utilisateurs.getReferenceById(DIRECTION)).thenReturn(direction);
        when(conversations.save(any(Conversation.class))).thenAnswer(i -> {
            Conversation c = i.getArgument(0);
            if (c.getId() == null) {
                c.setId(77L);
            }
            return c;
        });
        when(messages.deLaConversation(77L)).thenReturn(List.of());
    }

    private NouvelleConversationRequest requete(long destinataire) {
        return new NouvelleConversationRequest(ELEVE, destinataire, "  Devoirs  ", "  Bonjour  ");
    }

    @Test
    void unParentVoitLesEnseignantsDeLaClasseEtLaDirection() {
        connecteEn(PARENT);
        List<ContactDto> contacts = service.contacts(ELEVE);
        assertThat(contacts).extracting(ContactDto::utilisateurId).containsExactly(PROF, DIRECTION);
        assertThat(contacts.get(0).role()).isEqualTo("Mathématiques, Physique");
        assertThat(contacts.get(1).role()).isEqualTo("Direction");
    }

    @Test
    void unEnseignantVoitLesParentsDeLEleve() {
        connecteEn(PROF);
        List<ContactDto> contacts = service.contacts(ELEVE);
        assertThat(contacts).hasSize(1);
        assertThat(contacts.get(0).utilisateurId()).isEqualTo(PARENT);
        assertThat(contacts.get(0).role()).isEqualTo("Mère");
    }

    @Test
    void unEtrangerNAPasDeContacts() {
        connecteEn(ETRANGER);
        assertThatThrownBy(() -> service.contacts(ELEVE)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unParentDemarreUneConversationAvecUnEnseignant() {
        connecteEn(PARENT);
        ConversationDto dto = service.demarrer(requete(PROF));

        assertThat(dto.sujet()).isEqualTo("Devoirs");
        assertThat(dto.interlocuteur()).isEqualTo("Moussa Ba");
        assertThat(dto.eleve()).isEqualTo("Fanta Diallo");
        assertThat(dto.nonLu()).isFalse();
        assertThat(dto.active()).isTrue();
        verify(messages).save(any(Message.class));
    }

    @Test
    void unEnseignantPeutEcrireAuParent() {
        connecteEn(PROF);
        ConversationDto dto = service.demarrer(requete(PARENT));
        assertThat(dto.interlocuteur()).isEqualTo("Awa Diallo");
        assertThat(dto.roleInterlocuteur()).isEqualTo("Parent");
    }

    @Test
    void unParentNeContactePasQuelquUnQuiNEstPasDeLEcole() {
        connecteEn(PARENT);
        assertThatThrownBy(() -> service.demarrer(requete(ETRANGER))).isInstanceOf(AccessDeniedException.class);
        verify(messages, never()).save(any());
    }

    @Test
    void unEtrangerNeContactePasLesParents() {
        connecteEn(ETRANGER);
        assertThatThrownBy(() -> service.demarrer(requete(PARENT))).isInstanceOf(AccessDeniedException.class);
    }

    private Conversation conversationExistante() {
        Conversation c = new Conversation();
        c.setId(77L);
        c.setEleve(eleve);
        c.setParent(parent);
        c.setPersonnel(prof);
        c.setSujet("Devoirs");
        c.setCreatedAt(Instant.parse("2026-10-28T10:00:00Z"));
        c.setDernierMessageLe(Instant.parse("2026-10-28T10:00:00Z"));
        c.setDernierAuteurId(PARENT);
        c.setDernierExtrait("Bonjour");
        c.setParentLuLe(Instant.parse("2026-10-28T10:00:00Z"));
        when(conversations.findById(77L)).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void unTiersNeVoitPasLaConversation() {
        conversationExistante();
        connecteEn(ETRANGER);
        assertThatThrownBy(() -> service.ouvrir(77L)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("introuvable");
        assertThatThrownBy(() -> service.repondre(77L, "Salut")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void ouvrirMarqueLaConversationCommeLue() {
        Conversation c = conversationExistante();
        connecteEn(PROF);
        assertThat(c.nonLuPar(PROF)).isTrue();

        ConversationDto dto = service.ouvrir(77L);

        assertThat(dto.nonLu()).isFalse();
        assertThat(c.nonLuPar(PROF)).isFalse();
    }

    @Test
    void uneReponseRendLaConversationNonLuePourLAutre() {
        Conversation c = conversationExistante();
        connecteEn(PROF);
        service.repondre(77L, "Bonjour Madame");

        assertThat(c.getDernierAuteurId()).isEqualTo(PROF);
        assertThat(c.nonLuPar(PARENT)).isTrue();
        assertThat(c.nonLuPar(PROF)).isFalse();
        assertThat(c.getDernierMessageLe()).isEqualTo(Instant.parse("2026-10-29T10:00:00Z"));
    }

    @Test
    void siLEnseignantQuitteLaClasseOnNePeutPlusRepondre() {
        conversationExistante();
        when(affectations.existsByClasseIdAndEnseignantUtilisateurId(CLASSE, PROF)).thenReturn(false);
        connecteEn(PARENT);

        assertThatThrownBy(() -> service.repondre(77L, "Bonjour")).isInstanceOf(ResponseStatusException.class);
        assertThat(service.ouvrir(77L).active()).isFalse();
    }

    @Test
    void lesMessagesTropLongsSontResumes() {
        connecteEn(PARENT);
        ConversationDto dto = service.demarrer(
                new NouvelleConversationRequest(ELEVE, PROF, "Sujet", "x".repeat(300)));
        assertThat(dto.apercu()).hasSize(140).endsWith("…");
    }

    @Test
    void ledestinatairePeutEtrePrevenuParEmailSansLeContenu() {
        connecteEn(PARENT);
        service.demarrer(new NouvelleConversationRequest(ELEVE, PROF, "Devoirs", "Texte confidentiel"));

        org.mockito.ArgumentCaptor<String> texte = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mail).envoyer(org.mockito.ArgumentMatchers.eq("moussa@example.org"),
                org.mockito.ArgumentMatchers.contains("Awa Diallo"), texte.capture());
        assertThat(texte.getValue()).contains("/messages?c=77").doesNotContain("Texte confidentiel");
    }

    @Test
    void pasDeNouvelEmailTantQueLAutreNAPasLu() {
        Conversation c = conversationExistante();
        c.setDernierAuteurId(PROF);
        c.setDernierMessageLe(Instant.parse("2026-10-28T12:00:00Z"));
        connecteEn(PROF);

        service.repondre(77L, "Un deuxième message");

        verify(mail, never()).envoyer(anyString(), anyString(), anyString());
    }

    @Test
    void unEmailQuiEchoueNeBloquePasLEnvoi() {
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP")).when(mail).envoyer(anyString(), anyString(), anyString());
        connecteEn(PARENT);
        assertThat(service.demarrer(requete(PROF)).id()).isEqualTo(77L);
    }
}
