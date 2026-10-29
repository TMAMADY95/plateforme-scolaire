package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationEnseignant;
import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Conversation;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.LienParental;
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
import com.plateforme.scolaire.web.dto.ConversationDto.MessageDto;
import com.plateforme.scolaire.web.dto.NouvelleConversationRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Messagerie parent ↔ personnel, à propos d'un élève. Règles :
 * <ul>
 *   <li>un parent écrit aux enseignants de la classe actuelle de son enfant et à la direction / au personnel
 *       administratif de l'établissement ;</li>
 *   <li>ces personnes peuvent écrire aux parents de l'élève ;</li>
 *   <li>seuls les deux participants lisent la conversation ; si la relation disparaît (changement de classe,
 *       départ), elle reste lisible mais on n'y répond plus.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MessagerieService {

    static final int MAX_CONVERSATIONS = 100;
    private static final Set<Role> ROLES_DIRECTION = Set.of(Role.DIRECTION_ETABLISSEMENT, Role.PERSONNEL_ADMIN);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EleveRepository eleveRepository;
    private final LienParentalRepository lienParentalRepository;
    private final InscriptionRepository inscriptionRepository;
    private final AffectationEnseignantRepository affectationEnseignantRepository;
    private final AffectationRoleRepository affectationRoleRepository;
    private final PerimetreService perimetreService;
    private final Clock clock;
    private final EnvoyeurMail envoyeurMail;

    @Value("${app.mail.url-front:http://localhost:5173}")
    private String urlFront = "http://localhost:5173";

    // ---------- Contacts ----------

    /** Les personnes que l'utilisateur courant peut contacter à propos de cet élève. */
    @Transactional(readOnly = true)
    public List<ContactDto> contacts(Long eleveId) {
        Long moi = moiId();
        if (estParentDe(eleveId, moi)) {
            return personnelContactable(eleveId).stream().filter(c -> !c.utilisateurId().equals(moi)).toList();
        }
        if (estPersonnelDe(eleveId, moi)) {
            return lienParentalRepository.findByEleveId(eleveId).stream()
                    .filter(l -> !l.getUtilisateur().getId().equals(moi))
                    .map(l -> new ContactDto(l.getUtilisateur().getId(), nom(l.getUtilisateur()), l.getRelation()))
                    .toList();
        }
        throw new AccessDeniedException("Vous n'avez pas de conversation possible à propos de cet élève");
    }

    // ---------- Conversations ----------

    public ConversationDto demarrer(NouvelleConversationRequest r) {
        Long moi = moiId();
        Long autre = r.destinataireId();
        if (moi.equals(autre)) {
            throw invalide("Vous ne pouvez pas vous écrire à vous-même");
        }
        Eleve eleve = eleveRepository.findById(r.eleveId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable"));
        Long parentId;
        Long personnelId;
        if (estParentDe(eleve.getId(), moi) && estPersonnelDe(eleve.getId(), autre)) {
            parentId = moi;
            personnelId = autre;
        } else if (estPersonnelDe(eleve.getId(), moi) && estParentDe(eleve.getId(), autre)) {
            parentId = autre;
            personnelId = moi;
        } else {
            throw new AccessDeniedException("Cette conversation n'est pas possible à propos de cet élève");
        }

        Instant maintenant = Instant.now(clock);
        Conversation c = new Conversation();
        c.setEleve(eleve);
        c.setParent(utilisateurRepository.getReferenceById(parentId));
        c.setPersonnel(utilisateurRepository.getReferenceById(personnelId));
        c.setSujet(r.sujet().trim());
        c.setCreatedAt(maintenant);
        ajouterMessage(c, utilisateurRepository.getReferenceById(moi), r.contenu().trim(), maintenant);
        return detail(c, moi, messageRepository.deLaConversation(c.getId()));
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> lister() {
        Long moi = moiId();
        return conversationRepository.deLUtilisateur(moi, PageRequest.of(0, MAX_CONVERSATIONS)).stream()
                .map(c -> resume(c, moi)).toList();
    }

    @Transactional(readOnly = true)
    public long nonLus() {
        Long moi = moiId();
        return conversationRepository.deLUtilisateur(moi, PageRequest.of(0, MAX_CONVERSATIONS)).stream()
                .filter(c -> c.nonLuPar(moi)).count();
    }

    /** Ouvre la conversation et la marque comme lue pour l'utilisateur courant. */
    public ConversationDto ouvrir(Long id) {
        Long moi = moiId();
        Conversation c = chargerPourParticipant(id, moi);
        c.marquerLu(moi, Instant.now(clock));
        return detail(c, moi, messageRepository.deLaConversation(id));
    }

    public ConversationDto repondre(Long id, String contenu) {
        Long moi = moiId();
        Conversation c = chargerPourParticipant(id, moi);
        if (!relationActive(c)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette conversation n'est plus active");
        }
        ajouterMessage(c, utilisateurRepository.getReferenceById(moi), contenu.trim(), Instant.now(clock));
        return detail(c, moi, messageRepository.deLaConversation(id));
    }

    // ---------- Interne ----------

    private void ajouterMessage(Conversation c, Utilisateur auteur, String contenu, Instant quand) {
        Utilisateur destinataire = c.getParent().getId().equals(auteur.getId()) ? c.getPersonnel() : c.getParent();
        // Un seul e-mail tant que le destinataire n'a pas relu : pas de rafale si l'autre écrit plusieurs fois.
        boolean prevenir = c.getId() == null || !c.nonLuPar(destinataire.getId());
        c.setDernierMessageLe(quand);
        c.setDernierAuteurId(auteur.getId());
        c.setDernierExtrait(contenu.length() <= 140 ? contenu : contenu.substring(0, 139) + "…");
        c.marquerLu(auteur.getId(), quand);
        Conversation enregistree = conversationRepository.save(c);
        Message m = new Message();
        m.setConversation(enregistree);
        m.setAuteur(auteur);
        m.setContenu(contenu);
        m.setCreatedAt(quand);
        messageRepository.save(m);
        if (prevenir) {
            prevenirParEmail(enregistree, auteur, destinataire);
        }
    }

    /** Prévient sans jamais révéler le contenu : le message se lit après connexion. Un échec d'envoi est sans effet. */
    private void prevenirParEmail(Conversation c, Utilisateur auteur, Utilisateur destinataire) {
        if (destinataire.getEmail() == null || destinataire.getEmail().isBlank()) {
            return;
        }
        try {
            envoyeurMail.envoyer(destinataire.getEmail(), "Nouveau message de " + nom(auteur),
                    "Bonjour,\n\n" + nom(auteur) + " vous a écrit à propos de " + nom(c.getEleve()) + " : « "
                            + c.getSujet() + " ».\n\nPour le lire et y répondre, connectez-vous : " + urlFront
                            + "/messages?c=" + c.getId() + "\n");
        } catch (RuntimeException e) {
            log.warn("E-mail de notification de message non envoyé (conversation {})", c.getId(), e);
        }
    }

    private Conversation chargerPourParticipant(Long id, Long moi) {
        Conversation c = conversationRepository.findById(id).orElse(null);
        if (c == null || !c.estParticipant(moi)) {
            // Même réponse dans les deux cas : on ne révèle pas l'existence des conversations des autres.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation introuvable");
        }
        return c;
    }

    private boolean relationActive(Conversation c) {
        Long eleveId = c.getEleve().getId();
        return estParentDe(eleveId, c.getParent().getId()) && estPersonnelDe(eleveId, c.getPersonnel().getId());
    }

    private boolean estParentDe(Long eleveId, Long utilisateurId) {
        return lienParentalRepository.existsByEleveIdAndUtilisateurId(eleveId, utilisateurId);
    }

    /** Enseigne dans la classe actuelle de l'élève, ou dirige / administre son établissement. */
    private boolean estPersonnelDe(Long eleveId, Long utilisateurId) {
        Inscription inscription = inscriptionActive(eleveId);
        if (inscription == null) {
            return false;
        }
        if (inscription.getClasse() != null
                && affectationEnseignantRepository.existsByClasseIdAndEnseignantUtilisateurId(
                inscription.getClasse().getId(), utilisateurId)) {
            return true;
        }
        Long etablissementId = inscription.getEtablissement().getId();
        return affectationRoleRepository.findByUtilisateurId(utilisateurId).stream()
                .anyMatch(a -> ROLES_DIRECTION.contains(a.getRole()) && a.getEtablissement() != null
                        && a.getEtablissement().getId().equals(etablissementId));
    }

    private Inscription inscriptionActive(Long eleveId) {
        return inscriptionRepository.findByEleveIdAndStatutOrderByAnneeScolaireDateDebutDesc(eleveId, "ACCEPTEE")
                .stream().findFirst().orElse(null);
    }

    private List<ContactDto> personnelContactable(Long eleveId) {
        Inscription inscription = inscriptionActive(eleveId);
        if (inscription == null) {
            return List.of();
        }
        // Un contact par personne ; un enseignant de plusieurs matières les voit réunies.
        Map<Long, ContactDto> contacts = new LinkedHashMap<>();
        if (inscription.getClasse() != null) {
            Map<Long, List<String>> matieres = new LinkedHashMap<>();
            Map<Long, Utilisateur> enseignants = new LinkedHashMap<>();
            for (AffectationEnseignant a : affectationEnseignantRepository.findByClasseId(inscription.getClasse().getId())) {
                Utilisateur u = a.getEnseignant().getUtilisateur();
                enseignants.put(u.getId(), u);
                matieres.computeIfAbsent(u.getId(), k -> new ArrayList<>()).add(a.getMatiere().getLibelle());
            }
            enseignants.forEach((uid, u) -> contacts.put(uid,
                    new ContactDto(uid, nom(u), String.join(", ", matieres.get(uid).stream().distinct().toList()))));
        }
        for (AffectationRole a : affectationRoleRepository.rechercherComptes(
                inscription.getEtablissement().getId(), ROLES_DIRECTION, "")) {
            Utilisateur u = a.getUtilisateur();
            contacts.putIfAbsent(u.getId(), new ContactDto(u.getId(), nom(u),
                    a.getRole() == Role.DIRECTION_ETABLISSEMENT ? "Direction" : "Personnel administratif"));
        }
        return List.copyOf(contacts.values());
    }

    private ConversationDto resume(Conversation c, Long moi) {
        return construire(c, moi, c.getDernierExtrait(), List.of(), false);
    }

    private ConversationDto detail(Conversation c, Long moi, List<Message> messages) {
        List<MessageDto> dtos = messages.stream()
                .map(m -> new MessageDto(m.getId(), m.getAuteur().getId(), nom(m.getAuteur()),
                        m.getAuteur().getId().equals(moi), m.getContenu(), m.getCreatedAt()))
                .toList();
        return construire(c, moi, c.getDernierExtrait(), dtos, relationActive(c));
    }

    private ConversationDto construire(Conversation c, Long moi, String apercu, List<MessageDto> messages,
                                       boolean active) {
        boolean jeSuisParent = c.getParent().getId().equals(moi);
        Utilisateur autre = jeSuisParent ? c.getPersonnel() : c.getParent();
        String role = jeSuisParent ? "Personnel de l'école" : "Parent";
        return new ConversationDto(c.getId(), c.getEleve().getId(),
                (c.getEleve().getPrenom() + " " + c.getEleve().getNom()).trim(), c.getSujet(), nom(autre), role,
                c.getDernierMessageLe(), apercu, c.nonLuPar(moi), active, messages);
    }

    private Long moiId() {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        if (p == null) {
            throw new AccessDeniedException("Authentification requise");
        }
        return p.getId();
    }

    private static String nom(Eleve e) {
        return (e.getPrenom() + " " + e.getNom()).trim();
    }

    private static String nom(Utilisateur u) {
        return (u.getPrenom() + " " + u.getNom()).trim();
    }

    private static ResponseStatusException invalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
