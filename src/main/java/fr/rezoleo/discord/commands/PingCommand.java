package fr.rezoleo.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PingCommand implements BotCommand {

    private final List<String> pingList;

    public PingCommand(List<String> pingList) {
        this.pingList = pingList;
    }

    private static List<PingResult> checkWebsitesStatus(List<String> urls, int timeoutSeconds) {

        try (var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds)) // Timeout pour établir la connexion
                .followRedirects(HttpClient.Redirect.NORMAL) // Suivre les redirections standard
                .build()) {

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

                Queue<PingResult> results = new ConcurrentLinkedQueue<>();

                var futures = urls.stream()
                        .map(url -> executor.submit(createPingTask(url, httpClient, timeoutSeconds, results)))
                        .toList();

                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'attente d'une tâche de ping : " + e.getMessage());
                    }
                }
                return results.stream().sorted(Comparator.comparing(PingResult::url)).toList();
            }
        }
    }

    private static Runnable createPingTask(String url, HttpClient httpClient, int timeoutSeconds, Collection<PingResult> results) {
        return () -> {
            PingResult result = new PingResult.UnexpectedError(url, null);
            try {
                URI uri = new URI(url);

                // S'assurer que le schéma est HTTP ou HTTPS (HttpClient ne gère pas d'autres protocoles comme ftp)
                if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                    result = new PingResult.UnexpectedError(url, new UnsupportedOperationException("not http / https"));
                } else {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(uri)
                            .method("HEAD", HttpRequest.BodyPublishers.noBody()) // Ou .GET() si HEAD n'est pas supporté par le serveur
                            .timeout(Duration.ofSeconds(timeoutSeconds))
                            .build();

                    HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

                    // Considérer "en ligne" si le code statut est 2xx (succès) ou 3xx (redirection)
                    int statusCode = response.statusCode();
                    if (statusCode >= 200 && statusCode < 400) {
                        result = new PingResult.Ok(url);
                    } else {
                        result = new PingResult.ErrorCode(url, statusCode);
                    }
                }

            } catch (URISyntaxException e) {
                result = new PingResult.MalformedURI(url, e);
            } catch (HttpTimeoutException e) {
                result = new PingResult.Timeout(url, e);
            } catch (InterruptedException e) {
                result = new PingResult.UnexpectedError(url, e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                result = new PingResult.UnexpectedError(url, e);
            } finally {
                results.add(result);
            }
        };
    }

    @Override
    public ApplicationCommandRequest getRequest() {
        return ApplicationCommandRequest.builder()
                .name("ping")
                .description("Ping les sites du Rézo")
                .build();
    }

    @Override
    public void process(ChatInputInteractionEvent event) {
        Thread.ofVirtual().start(() -> {
            List<PingResult> status = checkWebsitesStatus(this.pingList, 10);

            EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                    .title("🌐 Rézoléo Ping Report")
                    .color(Color.BLUE);

            status.forEach(result -> {
                String emoji = switch (result) {
                    case PingResult.ErrorCode errorCode -> "🔴";
                    case PingResult.MalformedURI malformedURI -> "⚠️";
                    case PingResult.Ok ok -> "🟢";
                    case PingResult.Timeout timeout -> "⏱️";
                    case PingResult.UnexpectedError unexpectedError -> "❗";
                };

                //todo record deconstruction
                String message = switch (result) {
                    case PingResult.ErrorCode errorCode -> "HTTP error code: " + errorCode.code();
                    case PingResult.MalformedURI malformedURI ->
                            "Malformed URL: " + malformedURI.exception().getMessage();
                    case PingResult.Ok ok -> "Online";
                    case PingResult.Timeout timeout -> "Timeout: " + timeout.exception().getMessage();
                    case PingResult.UnexpectedError unexpectedError ->
                            "Unexpected error: " + unexpectedError.exception().getMessage();
                };

                builder.addField(emoji + " " + result.url(), message, false);
            });

            event.createFollowup().withEmbeds(builder.build()).block();
        });
    }

    private sealed interface PingResult {

        String url();

        record Ok(String url) implements PingResult {
        }

        record ErrorCode(String url, int code) implements PingResult {
        }

        record MalformedURI(String url, URISyntaxException exception) implements PingResult {
        }

        record Timeout(String url, HttpTimeoutException exception) implements PingResult {
        }

        record UnexpectedError(String url, Exception exception) implements PingResult {
        }
    }
}
