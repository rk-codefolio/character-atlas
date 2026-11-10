package dev.characteratlas.archipelago.moderation.infrastructure;


import tools.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DiscordModerationAccess {
    public static final String USER_SESSION = "moderation.discordUser";
    private static final String FLOW_SESSION = "moderation.discordFlow";
    private static final String DISCORD = "https://discord.com";
    private final String origin;
    private final String clientId;
    private final String secret;
    private final String ownerId;
    private final Set<String> moderatorIds;
    private final SecureRandom random = new SecureRandom();
    private final RestClient discord;

    public DiscordModerationAccess(@Value("${catalog.frontend-origin}") String origin,
                            @Value("${catalog.discord.client-id}") String clientId,
                            @Value("${catalog.discord.client-secret}") String secret,
                            @Value("${catalog.discord.owner-id}") String ownerId,
                            @Value("${catalog.discord.moderator-ids}") String moderatorIds) {
        this.origin = origin;
        this.clientId = clientId;
        this.secret = secret;
        this.ownerId = ownerId;
        this.moderatorIds = Arrays.stream(moderatorIds.split(",")).map(String::strip)
                .filter(DiscordModerationAccess::validId).collect(Collectors.toUnmodifiableSet());
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(8));
        discord = RestClient.builder().baseUrl(DISCORD).requestFactory(factory).build();
    }

    public Access access(HttpServletRequest request) {
        var session = request.getSession(false);
        String id = session == null ? null : (String) session.getAttribute(USER_SESSION);
        boolean configured = enabled();
        boolean owner = configured && validId(id) && ownerId.equals(id);
        return new Access(configured, owner, configured && !owner && validId(id) && moderatorIds.contains(id));
    }

    public String requireModerator(HttpServletRequest request) {
        Access access = access(request);
        if (!access.canModerate()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return (String) request.getSession(false).getAttribute(USER_SESSION);
    }

    public String requireOwner(HttpServletRequest request) {
        if (!access(request).owner()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return requireModerator(request);
    }

    public void requireOrigin(HttpServletRequest request) {
        if (!origin.equals(request.getHeader("Origin"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public URI start(HttpServletRequest request) {
        if (!enabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        request.getSession().setAttribute(FLOW_SESSION, new Flow(state, Instant.now().plusSeconds(300)));
        return UriComponentsBuilder.fromUriString(DISCORD + "/oauth2/authorize")
                .queryParam("response_type", "code").queryParam("scope", "identify")
                .queryParam("client_id", clientId).queryParam("redirect_uri", callback())
                .queryParam("state", state).build().encode().toUri();
    }

    public URI finish(HttpServletRequest request, String state, String code) {
        var session = request.getSession(false);
        if (session == null || !enabled()) return landing("expired");
        Flow flow;
        synchronized (session) {
            flow = (Flow) session.getAttribute(FLOW_SESSION);
            if (flow == null || !flow.state().equals(state) || Instant.now().isAfter(flow.expires())) {
                return landing("expired");
            }
            session.removeAttribute(FLOW_SESSION);
        }
        if (code == null || code.isBlank()) return landing("cancelled");
        String id;
        try {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("client_id", clientId);
            form.add("client_secret", secret);
            form.add("grant_type", "authorization_code");
            form.add("code", code);
            form.add("redirect_uri", callback());
            JsonNode token = discord.post().uri("/api/v10/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
            String bearer = token == null ? "" : token.path("access_token").asString("");
            if (bearer.isBlank()) return landing("error");
            JsonNode user = discord.get().uri("/api/v10/users/@me").headers(headers -> headers.setBearerAuth(bearer))
                    .retrieve().body(JsonNode.class);
            id = user == null ? null : user.path("id").asString(null);
        } catch (RestClientException error) {
            return landing("error");
        }
        if (!validId(id) || !(ownerId.equals(id) || moderatorIds.contains(id))) return landing("denied");
        request.changeSessionId();
        session.setAttribute(USER_SESSION, id);
        return landing(null);
    }

    private URI landing(String result) {
        return URI.create(origin + "/?admin=1" + (result == null ? "" : "&auth=" + result));
    }

    private boolean enabled() {
        return validId(clientId) && !secret.isBlank() && (validId(ownerId) || !moderatorIds.isEmpty());
    }

    private String callback() {
        return origin + "/api/admin/discord/callback";
    }

    private static boolean validId(String value) {
        return value != null && value.matches("[0-9]{1,20}");
    }

    public record Access(boolean configured, boolean owner, boolean moderator) {
        public boolean canModerate() { return owner || moderator; }
    }

    private record Flow(String state, Instant expires) {}
}
