package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.MessagerieService;
import com.plateforme.scolaire.web.dto.ContactDto;
import com.plateforme.scolaire.web.dto.ConversationDto;
import com.plateforme.scolaire.web.dto.MessageRequest;
import com.plateforme.scolaire.web.dto.NouvelleConversationRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Messagerie parent ↔ personnel. Les règles d'accès (participants, relation avec l'élève) sont dans le service. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MessagerieController {

    private final MessagerieService messagerieService;

    @GetMapping("/api/messagerie/contacts")
    public List<ContactDto> contacts(@RequestParam Long eleveId) {
        return messagerieService.contacts(eleveId);
    }

    @GetMapping("/api/messagerie/conversations")
    public List<ConversationDto> lister() {
        return messagerieService.lister();
    }

    @PostMapping("/api/messagerie/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDto demarrer(@Valid @RequestBody NouvelleConversationRequest requete) {
        return messagerieService.demarrer(requete);
    }

    /** Ouvre la conversation et la marque comme lue. */
    @GetMapping("/api/messagerie/conversations/{id}")
    public ConversationDto ouvrir(@PathVariable Long id) {
        return messagerieService.ouvrir(id);
    }

    @PostMapping("/api/messagerie/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDto repondre(@PathVariable Long id, @Valid @RequestBody MessageRequest requete) {
        return messagerieService.repondre(id, requete.contenu());
    }

    @GetMapping("/api/messagerie/non-lus")
    public Map<String, Long> nonLus() {
        return Map.of("nonLus", messagerieService.nonLus());
    }
}
