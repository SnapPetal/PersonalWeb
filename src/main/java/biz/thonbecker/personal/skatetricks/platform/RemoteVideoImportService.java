package biz.thonbecker.personal.skatetricks.platform;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RemoteVideoImportService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);
    private static final int MAX_RESOLUTION_DEPTH = 2;
    private static final List<String> SUPPORTED_VIDEO_TYPES = List.of(
            "video/mp4",
            "video/quicktime",
            "video/webm",
            "video/x-msvideo",
            "video/x-matroska",
            "application/octet-stream");
    private static final Pattern YOUTUBE_MP4_URL_PATTERN = Pattern.compile(
            "\"url\"\\s*:\\s*\"([^\\\"]*(?:googlevideo(?:\\\\u002E|\\.)com|videoplayback)[^\\\"]*)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final List<Pattern> SOCIAL_VIDEO_URL_PATTERNS = List.of(
            Pattern.compile("\"playable_url_quality_hd\"\\s*:\\s*\"([^\\\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"playable_url\"\\s*:\\s*\"([^\\\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"browser_native_sd_url\"\\s*:\\s*\"([^\\\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"browser_native_hd_url\"\\s*:\\s*\"([^\\\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"video_url\"\\s*:\\s*\"([^\\\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"contentUrl\"\\s*:\\s*\"([^\\\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "(https?:\\\\?/\\\\?/[^\\\"'\\s>]+\\.(?:mp4|mov|webm)(?:[^\\\"'\\s<]*)?)",
                    Pattern.CASE_INSENSITIVE));

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final SkatetricksObservability observability;

    RemoteVideoImportService(SkatetricksObservability observability) {
        this.observability = observability;
    }

    public DownloadedVideo downloadVideo(String sourceUrl, long maxBytes) throws RemoteVideoImportException {
        final var scope = observability.start("remote_import.download_video");
        try {
            URI uri = canonicalizeSocialUrl(validateUrl(sourceUrl));
            log.info(
                    "event=remote_import_started sourceHost={} sourcePath={} maxBytes={}",
                    uri.getHost(),
                    uri.getPath(),
                    maxBytes);
            ResolvedVideoSource resolvedSource = resolveVideoSource(uri, 0, null);
            log.info(
                    "event=remote_import_resolved sourceHost={} mediaHost={} referer={}",
                    uri.getHost(),
                    resolvedSource.uri().getHost(),
                    resolvedSource.referer());
            DownloadedVideo downloadedVideo = downloadResolvedVideo(resolvedSource, maxBytes);
            observability.recordPayloadSize(
                    "remote_download", downloadedVideo.bytes().length, "source", classifyHost(uri));
            observability.success(scope, "source", classifyHost(uri));
            return downloadedVideo;
        } catch (RemoteVideoImportException e) {
            observability.failure(scope, e, "source", "validation");
            throw e;
        } catch (RuntimeException e) {
            observability.failure(scope, e, "source", "runtime");
            throw e;
        }
    }

    private DownloadedVideo downloadResolvedVideo(ResolvedVideoSource resolvedSource, long maxBytes)
            throws RemoteVideoImportException {
        final var scope = observability.start("remote_import.download_resolved_video");
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(resolvedSource.uri())
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "PersonalWeb-Skatetricks/1.0")
                .GET();
        if (resolvedSource.referer() != null && !resolvedSource.referer().isBlank()) {
            requestBuilder.header("Referer", resolvedSource.referer());
        }
        HttpRequest request = requestBuilder.build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new RemoteVideoImportException(
                        RemoteVideoImportErrorCode.REMOTE_SERVER_ERROR,
                        "The remote server returned HTTP " + status + " while downloading the video.");
            }

            String contentType = response.headers().firstValue("content-type").orElse("application/octet-stream");
            String normalizedContentType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);

            if (normalizedContentType.startsWith("text/html")) {
                throw socialUrlException(resolvedSource.sourcePageUri());
            }
            if (SUPPORTED_VIDEO_TYPES.stream().noneMatch(normalizedContentType::equals)) {
                throw new RemoteVideoImportException(
                        RemoteVideoImportErrorCode.UNSUPPORTED_CONTENT_TYPE,
                        "The URL did not return a supported video file. Content type was " + contentType + ".");
            }

            long declaredLength =
                    response.headers().firstValueAsLong("content-length").orElse(-1L);
            if (declaredLength > maxBytes) {
                throw new RemoteVideoImportException(
                        RemoteVideoImportErrorCode.VIDEO_TOO_LARGE,
                        "The remote video is larger than the upload limit.");
            }

            byte[] bytes = readLimited(response.body(), maxBytes);
            String filename = determineFilename(
                    resolvedSource.uri(),
                    normalizedContentType,
                    response.headers().firstValue("content-disposition").orElse(null),
                    resolvedSource.filenameHint());
            observability.incrementStage("remote_download", "success", "contentType", normalizedContentType);
            observability.success(scope, "contentType", normalizedContentType);
            return new DownloadedVideo(bytes, filename, normalizedContentType);
        } catch (RemoteVideoImportException e) {
            observability.incrementStage(
                    "remote_download", "failure", "errorCode", e.code().name());
            observability.failure(scope, e, "errorCode", e.code().name());
            throw e;
        } catch (Exception e) {
            observability.incrementStage(
                    "remote_download", "failure", "errorCode", RemoteVideoImportErrorCode.DOWNLOAD_FAILED.name());
            observability.failure(scope, e, "errorCode", RemoteVideoImportErrorCode.DOWNLOAD_FAILED.name());
            throw new RemoteVideoImportException(
                    RemoteVideoImportErrorCode.DOWNLOAD_FAILED, "Failed to download the resolved video file.", e);
        }
    }

    private ResolvedVideoSource resolveVideoSource(URI sourceUri, int depth, URI referer)
            throws RemoteVideoImportException {
        final var scope = observability.start("remote_import.resolve_video_source");
        if (depth > MAX_RESOLUTION_DEPTH) {
            observability.incrementStage(
                    "remote_resolution",
                    "failure",
                    "errorCode",
                    RemoteVideoImportErrorCode.RESOLUTION_DEPTH_EXCEEDED.name());
            observability.failure(
                    scope, null, "errorCode", RemoteVideoImportErrorCode.RESOLUTION_DEPTH_EXCEEDED.name());
            throw new RemoteVideoImportException(
                    RemoteVideoImportErrorCode.RESOLUTION_DEPTH_EXCEEDED,
                    "Video URL resolution exceeded the maximum redirect depth.");
        }
        if (!requiresProviderResolution(sourceUri)) {
            observability.incrementStage("remote_resolution", "success", "provider", classifyHost(sourceUri));
            observability.success(scope, "provider", classifyHost(sourceUri));
            return new ResolvedVideoSource(sourceUri, referer != null ? referer.toString() : null, null, sourceUri);
        }

        log.info("event=provider_resolution_started host={} depth={}", sourceUri.getHost(), depth);
        String html = fetchHtml(sourceUri);
        String resolvedUrl = extractVideoUrlFromHtml(sourceUri, html);
        if (resolvedUrl == null || resolvedUrl.isBlank()) {
            observability.incrementStage(
                    "remote_resolution",
                    "failure",
                    "errorCode",
                    RemoteVideoImportErrorCode.SOCIAL_VIDEO_UNRESOLVED.name());
            observability.failure(
                    scope,
                    null,
                    "provider",
                    classifyHost(sourceUri),
                    "errorCode",
                    RemoteVideoImportErrorCode.SOCIAL_VIDEO_UNRESOLVED.name());
            throw unresolvedSocialUrlException(sourceUri);
        }

        URI resolvedUri = validateUrl(resolvedUrl);
        if (requiresProviderResolution(resolvedUri)) {
            return resolveVideoSource(resolvedUri, depth + 1, sourceUri);
        }
        log.info(
                "event=provider_resolution_completed provider={} resolvedHost={} depth={}",
                classifyHost(sourceUri),
                resolvedUri.getHost(),
                depth);
        observability.incrementStage("remote_resolution", "success", "provider", classifyHost(sourceUri));
        observability.success(scope, "provider", classifyHost(sourceUri));
        return new ResolvedVideoSource(resolvedUri, sourceUri.toString(), extractOgTitle(html), sourceUri);
    }

    private String fetchHtml(URI uri) throws RemoteVideoImportException {
        final var scope = observability.start("remote_import.fetch_provider_html");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "PersonalWeb-Skatetricks/1.0")
                .header("Accept", "text/html,application/xhtml+xml")
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                observability.incrementStage("provider_html_fetch", "failure", "status", Integer.toString(status));
                observability.failure(scope, null, "status", Integer.toString(status));
                throw new RemoteVideoImportException(
                        RemoteVideoImportErrorCode.REMOTE_SERVER_ERROR,
                        "The provider page returned HTTP " + status + ".");
            }
            observability.incrementStage("provider_html_fetch", "success", "provider", classifyHost(uri));
            observability.success(scope, "provider", classifyHost(uri));
            return response.body();
        } catch (RemoteVideoImportException e) {
            observability.failure(scope, e, "errorCode", e.code().name());
            throw e;
        } catch (Exception e) {
            observability.incrementStage(
                    "provider_html_fetch",
                    "failure",
                    "errorCode",
                    RemoteVideoImportErrorCode.PROVIDER_PAGE_FETCH_FAILED.name());
            observability.failure(scope, e, "errorCode", RemoteVideoImportErrorCode.PROVIDER_PAGE_FETCH_FAILED.name());
            throw new RemoteVideoImportException(
                    RemoteVideoImportErrorCode.PROVIDER_PAGE_FETCH_FAILED,
                    "Failed to load the provider page for video resolution.",
                    e);
        }
    }

    private static URI validateUrl(String sourceUrl) throws RemoteVideoImportException {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new RemoteVideoImportException(RemoteVideoImportErrorCode.MISSING_URL, "Video URL is required.");
        }
        try {
            URI uri = URI.create(sourceUrl.trim());
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new RemoteVideoImportException(
                        RemoteVideoImportErrorCode.INVALID_URL, "Only http and https video URLs are supported.");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new RemoteVideoImportException(
                        RemoteVideoImportErrorCode.INVALID_URL, "Video URL must include a host.");
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new RemoteVideoImportException(RemoteVideoImportErrorCode.INVALID_URL, "Invalid video URL.", e);
        }
    }

    private static byte[] readLimited(InputStream inputStream, long maxBytes) throws Exception {
        try (inputStream;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new RemoteVideoImportException(
                            RemoteVideoImportErrorCode.VIDEO_TOO_LARGE,
                            "The remote video is larger than the upload limit.");
                }
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    static String extractVideoUrlFromHtml(URI pageUri, String html) {
        if (html == null || html.isBlank()) {
            return null;
        }

        String metaVideo = firstNonBlank(
                extractMetaTagContent(html, "og:video:secure_url"),
                extractMetaTagContent(html, "og:video:url"),
                extractMetaTagContent(html, "og:video"),
                extractMetaTagContent(html, "twitter:player:stream"));
        if (looksLikeDownloadableVideoUrl(metaVideo)) {
            return normalizeExtractedUrl(pageUri, metaVideo);
        }

        String host = pageUri.getHost() == null ? "" : pageUri.getHost().toLowerCase(Locale.ROOT);
        if (isYouTubeHost(host)) {
            String youtubeUrl = extractByPattern(YOUTUBE_MP4_URL_PATTERN, html);
            if (looksLikeDownloadableVideoUrl(youtubeUrl)) {
                return normalizeExtractedUrl(pageUri, youtubeUrl);
            }
        }

        if (isInstagramHost(host) || isFacebookHost(host)) {
            for (Pattern pattern : SOCIAL_VIDEO_URL_PATTERNS) {
                String extracted = extractByPattern(pattern, html);
                if (looksLikeDownloadableVideoUrl(extracted)) {
                    return normalizeExtractedUrl(pageUri, extracted);
                }
            }
        }

        for (Pattern pattern : SOCIAL_VIDEO_URL_PATTERNS) {
            String extracted = extractByPattern(pattern, html);
            if (looksLikeDownloadableVideoUrl(extracted)) {
                return normalizeExtractedUrl(pageUri, extracted);
            }
        }

        return null;
    }

    private static String determineFilename(
            URI uri, String contentType, String contentDisposition, String filenameHint) {
        if (filenameHint != null && !filenameHint.isBlank()) {
            return ensureExtension(sanitizeFilename(filenameHint), contentType);
        }
        String filename = parseFilenameFromDisposition(contentDisposition);
        if (filename != null && !filename.isBlank()) {
            return filename;
        }

        String path = uri.getPath();
        if (path != null && !path.isBlank() && !path.endsWith("/")) {
            String pathName = path.substring(path.lastIndexOf('/') + 1);
            if (!pathName.isBlank()) {
                return ensureExtension(pathName, contentType);
            }
        }
        return "remote-video" + extensionFor(contentType);
    }

    private static String parseFilenameFromDisposition(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isBlank()) {
            return null;
        }
        for (String part : contentDisposition.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("filename=")) {
                String raw = trimmed.substring("filename=".length())
                        .replace("\"", "")
                        .trim();
                return raw.isBlank() ? null : raw;
            }
        }
        return null;
    }

    private static String ensureExtension(String filename, String contentType) {
        int dot = filename.lastIndexOf('.');
        if (dot > 0 && dot < filename.length() - 1) {
            return filename;
        }
        return filename + extensionFor(contentType);
    }

    private static String sanitizeFilename(String raw) {
        String sanitized = raw.replaceAll("[\\\\/:*?\"<>|]", " ").trim();
        sanitized = sanitized.replaceAll("\\s+", "-");
        return sanitized.isBlank() ? "remote-video" : sanitized;
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "video/quicktime" -> ".mov";
            case "video/webm" -> ".webm";
            case "video/x-msvideo" -> ".avi";
            case "video/x-matroska" -> ".mkv";
            default -> ".mp4";
        };
    }

    static boolean requiresProviderResolution(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return isInstagramHost(host) || isFacebookHost(host) || isYouTubeHost(host);
    }

    static URI canonicalizeSocialUrl(URI uri) {
        if (Objects.isNull(uri)) {
            return null;
        }

        final var host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!isFacebookHost(host)) {
            return uri;
        }

        final var path = uri.getPath() == null ? "" : uri.getPath();
        if (path.startsWith("/share/r/")) {
            final var normalizedPath = path.replaceFirst("^/share/r/", "/reel/");
            return URI.create(uri.getScheme() + "://" + uri.getHost() + normalizedPath);
        }

        return uri;
    }

    private static boolean isInstagramHost(String host) {
        return matchesDomain(host, "instagram.com");
    }

    private static boolean isFacebookHost(String host) {
        return matchesDomain(host, "facebook.com") || matchesDomain(host, "fb.watch");
    }

    private static boolean isYouTubeHost(String host) {
        return matchesDomain(host, "youtube.com") || matchesDomain(host, "youtu.be");
    }

    private static boolean matchesDomain(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }

    private static String extractOgTitle(String html) {
        return extractMetaTagContent(html, "og:title");
    }

    private static String extractMetaTagContent(String html, String propertyName) {
        String quotedProperty = Pattern.quote(propertyName);
        Pattern propertyFirst = Pattern.compile(
                "<meta[^>]+(?:property|name)=[\"']" + quotedProperty + "[\"'][^>]+content=[\"']([^\"']+)[\"'][^>]*>",
                Pattern.CASE_INSENSITIVE);
        Pattern contentFirst = Pattern.compile(
                "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+(?:property|name)=[\"']" + quotedProperty + "[\"'][^>]*>",
                Pattern.CASE_INSENSITIVE);
        String extracted = extractByPattern(propertyFirst, html);
        if (extracted != null) {
            return normalizeExtractedUrl(null, extracted);
        }
        return normalizeExtractedUrl(null, extractByPattern(contentFirst, html));
    }

    private static String extractByPattern(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private static boolean looksLikeDownloadableVideoUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalizeExtractedUrl(null, value);
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("//");
    }

    private static String normalizeExtractedUrl(URI pageUri, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace("&amp;", "&");
        normalized = decodeUnicodeEscapes(normalized);
        normalized = normalized.replace("\\/", "/");
        normalized = normalized.replace("\\&", "&");
        if (normalized.startsWith("//")) {
            String scheme = pageUri != null && pageUri.getScheme() != null ? pageUri.getScheme() : "https";
            return scheme + ":" + normalized;
        }
        return normalized;
    }

    private static String decodeUnicodeEscapes(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            if (i + 5 < value.length() && value.charAt(i) == '\\' && value.charAt(i + 1) == 'u') {
                String hex = value.substring(i + 2, i + 6);
                if (hex.chars().allMatch(ch -> Character.digit(ch, 16) != -1)) {
                    result.append((char) Integer.parseInt(hex, 16));
                    i += 5;
                    continue;
                }
            }
            result.append(value.charAt(i));
        }
        return result.toString();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String classifyHost(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (isInstagramHost(host)) {
            return "instagram";
        }
        if (isFacebookHost(host)) {
            return "facebook";
        }
        if (isYouTubeHost(host)) {
            return "youtube";
        }
        return host.isBlank() ? "unknown" : "direct";
    }

    private static RemoteVideoImportException socialUrlException(URI uri) {
        if (requiresProviderResolution(uri)) {
            return unresolvedSocialUrlException(uri);
        }
        return new RemoteVideoImportException(
                RemoteVideoImportErrorCode.HTML_INSTEAD_OF_VIDEO,
                "The URL returned an HTML page instead of a downloadable video file.");
    }

    private static RemoteVideoImportException unresolvedSocialUrlException(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (isInstagramHost(host) || isFacebookHost(host)) {
            return new RemoteVideoImportException(
                    RemoteVideoImportErrorCode.SOCIAL_VIDEO_UNRESOLVED,
                    "Could not resolve a downloadable Instagram or Facebook video from that page URL. The post may be private, geo-restricted, or require authentication.");
        }
        if (isYouTubeHost(host)) {
            return new RemoteVideoImportException(
                    RemoteVideoImportErrorCode.SOCIAL_VIDEO_UNRESOLVED,
                    "Could not resolve a downloadable YouTube video stream from that page URL. The video may require a provider-specific extractor or have stream protection enabled.");
        }
        return new RemoteVideoImportException(
                RemoteVideoImportErrorCode.SOCIAL_VIDEO_UNRESOLVED,
                "Could not resolve a downloadable video from that page URL.");
    }

    private record ResolvedVideoSource(URI uri, String referer, String filenameHint, URI sourcePageUri) {}

    public record DownloadedVideo(byte[] bytes, String filename, String contentType) {}

    public enum RemoteVideoImportErrorCode {
        MISSING_URL,
        INVALID_URL,
        PROVIDER_PAGE_FETCH_FAILED,
        RESOLUTION_DEPTH_EXCEEDED,
        SOCIAL_VIDEO_UNRESOLVED,
        HTML_INSTEAD_OF_VIDEO,
        UNSUPPORTED_CONTENT_TYPE,
        REMOTE_SERVER_ERROR,
        VIDEO_TOO_LARGE,
        DOWNLOAD_FAILED
    }

    public static class RemoteVideoImportException extends Exception {
        private final RemoteVideoImportErrorCode code;

        public RemoteVideoImportException(RemoteVideoImportErrorCode code, String message) {
            super(message);
            this.code = code;
        }

        public RemoteVideoImportException(RemoteVideoImportErrorCode code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }

        public RemoteVideoImportErrorCode code() {
            return code;
        }
    }
}
