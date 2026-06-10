package biz.thonbecker.personal.landscape.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LandscapeImageGenerationServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final byte[] PNG_IMAGE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};

    @Test
    void sendsResponsesImageGenerationRequestAndExtractsGeneratedImage() throws Exception {
        final var httpClient = new StubHttpClient(200, """
                {
                  "output": [
                    {"type": "message", "content": []},
                    {"type": "image_generation_call", "result": "generated-base64-image"}
                  ]
                }
                """);
        final var service = serviceFor(httpClient);

        final var result = service.generateSeasonalImage(
                PNG_IMAGE,
                "Spring",
                List.of(new LandscapeImageGenerationService.PlantPlacementPrompt("ACRU", "Red maple", 34.5, 62.0)));

        assertEquals("generated-base64-image", result);
        assertEquals(1, httpClient.requestCount());
        assertEquals(
                "https://openai.test/v1/responses",
                httpClient.lastRequest().uri().toString());

        final var requestJson = OBJECT_MAPPER.readTree(httpClient.lastRequestBody());
        assertEquals("gpt-test-image-model", requestJson.path("model").asText());
        assertEquals(
                "image_generation", requestJson.path("tool_choice").path("type").asText());
        assertEquals(
                "image_generation",
                requestJson.path("tools").path(0).path("type").asText());

        final var content = requestJson.path("input").path(0).path("content");
        assertEquals("input_text", content.path(0).path("type").asText());
        assertTrue(content.path(0).path("text").asText().contains("Add Red maple (ACRU)"));
        assertTrue(content.path(0).path("text").asText().contains("x=34.5%"));
        assertTrue(content.path(0).path("text").asText().contains("y=62.0%"));
        assertEquals("input_image", content.path(1).path("type").asText());
        assertTrue(content.path(1).path("image_url").asText().startsWith("data:image/png;base64,"));
    }

    @Test
    void returnsNullWhenSuccessfulResponseDoesNotContainGeneratedImage() {
        final var httpClient = new StubHttpClient(200, """
                {"output": [{"type": "message", "content": []}]}
                """);
        final var service = serviceFor(httpClient);

        final var result = service.generateSeasonalImage(PNG_IMAGE, "Summer", List.of());

        assertNull(result);
        assertEquals(1, httpClient.requestCount());
    }

    @Test
    void cachesOrganizationVerificationDenialAndSkipsFutureRequests() {
        final var httpClient = new StubHttpClient(403, """
                {"error":{"message":"Your organization must be verified to use the model."}}
                """);
        final var service = serviceFor(httpClient);

        assertNull(service.generateSeasonalImage(PNG_IMAGE, "Fall", List.of()));
        assertNull(service.generateSeasonalImage(PNG_IMAGE, "Winter", List.of()));

        assertEquals(1, httpClient.requestCount());
    }

    @Test
    void skipsRequestWhenOpenAiApiKeyIsMissing() {
        final var httpClient = new StubHttpClient(200, "{}");
        final var service = serviceFor(httpClient);
        ReflectionTestUtils.setField(service, "openAiApiKey", " ");

        final var result = service.generateSeasonalImage(PNG_IMAGE, "Spring", List.of());

        assertNull(result);
        assertEquals(0, httpClient.requestCount());
    }

    private LandscapeImageGenerationService serviceFor(final HttpClient httpClient) {
        final var service = new LandscapeImageGenerationService(
                URI.create("https://openai.test/v1/responses"), OBJECT_MAPPER, httpClient);
        ReflectionTestUtils.setField(service, "imageResponsesModelName", "gpt-test-image-model");
        ReflectionTestUtils.setField(service, "openAiApiKey", "test-api-key");
        return service;
    }

    private static String readBodyPublisher(final HttpRequest request) throws IOException {
        final var publisher = request.bodyPublisher().orElseThrow();
        final var output = new ByteArrayOutputStream();
        final var latch = new CountDownLatch(1);
        final var failure = new AtomicReference<Throwable>();

        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(final Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final ByteBuffer item) {
                final var bytes = new byte[item.remaining()];
                item.get(bytes);
                output.writeBytes(bytes);
            }

            @Override
            public void onError(final Throwable throwable) {
                failure.set(throwable);
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while reading request body", e);
        }
        if (failure.get() != null) {
            throw new IOException("Failed to read request body", failure.get());
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private static final class StubHttpClient extends HttpClient {

        private final int statusCode;
        private final String responseBody;
        private final AtomicInteger requestCount = new AtomicInteger();
        private HttpRequest lastRequest;
        private String lastRequestBody;

        private StubHttpClient(final int statusCode, final String responseBody) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        private int requestCount() {
            return requestCount.get();
        }

        private HttpRequest lastRequest() {
            assertNotNull(lastRequest);
            return lastRequest;
        }

        private String lastRequestBody() {
            assertNotNull(lastRequestBody);
            return lastRequestBody;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.of(Duration.ofSeconds(2));
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(
                final HttpRequest request, final HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException {
            requestCount.incrementAndGet();
            lastRequest = request;
            lastRequestBody = readBodyPublisher(request);
            return (HttpResponse<T>) new StubHttpResponse(request, statusCode, responseBody);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                final HttpRequest request, final HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                final HttpRequest request,
                final HttpResponse.BodyHandler<T> responseBodyHandler,
                final HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException();
        }
    }

    private record StubHttpResponse(HttpRequest request, int statusCode, String body) implements HttpResponse<String> {

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (left, right) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
