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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class RemoteVideoImportService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);
    private static final int MAX_RESOLUTION_DEPTH = 2;
    private static final List<String> SUPPORTED_VIDEO_TYPES = List.of(
            "video/mp4", "video/quicktime", "video/webm", "video/x-msvideo", "video/x-matroska", "application/octet-stream");
    private static final Pattern YOUTUBE_MP4_URL_PATTERN = Pattern.compile(
            "\"url\"\\s*:\\s*\"(https?:\\\\?/\\\\?/[^\\\"]+(?:googlevideo\\\\.com|videoplayback)[^\\\"]+)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final List<Pattern> SOCIAL_VIDEO_URL_PATTERNS = List.of(
            Pattern.compile("\"playable_url_quality_hd\"\\s*:\\s*\"(https?:\\\\?/\\\\?/[^\\\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"playable_url\"\\s*:\\s*\"(https?:\\\\?/\\\\?/[^\\\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"video_url\"\\s*:\\s*\"(https?:\\\\?/\\\\?/[^\\\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"contentUrl\"\\s*:\\s*\"(https?:\\\\?/\\\\?/[^\\\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(https?:\\\\?/\\\\?/[^\\\"'\\s>]+\\.(?:mp4|mov|webm)(?:[^\\\"'\\s<]*)?)", Pattern.CASE_INSENSITIVE));

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public DownloadedVideo downloadVideo(String sourceUrl, long maxBytes) throws RemoteVideoImportException {
        URI uri = validateUrl(sourceUrl);
        ResolvedVideoSource resolvedSource = resolveVideoSource(uri, 0, null);
        return downloadResolvedVideo(resolvedSource, maxBytes);
    }

    private DownloadedVideo downloadResolvedVideo(ResolvedVideoSource resolvedSource, long maxBytes)
            throws RemoteVideoImportException {
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
                throw new RemoteVideoImportException("Remote server returned HTTP " + status);
            }

            String contentType = response.headers()
                    .firstValue("content-type")
                    .orElse("application/octet-stream");
            String normalizedContentType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);

            if (normalizedContentType.startsWith("text/html")) {
                throw socialUrlException(resolvedSource.sourcePageUri());
            }
            if (SUPPORTED_VIDEO_TYPES.stream().noneMatch(normalizedContentType::equals)) {
                throw new RemoteVideoImportException("URL did not return a supported video content type: " + contentType);
            }

            long declaredLength = response.headers()
                    .firstValueAsLong("content-length")
                    .orElse(-1L);
            if (declaredLength > maxBytes) {
                throw new RemoteVideoImportException("Remote video exceeds max size of " + maxBytes + " bytes");
            }

            byte[] bytes = readLimited(response.body(), maxBytes);
            String filename = determineFilename(
                    resolvedSource.uri(),
                    normalizedContentType,
                    response.headers().firstValue("content-disposition").orElse(null),
                    resolvedSource.filenameHint());
            return new DownloadedVideo(bytes, filename, normalizedContentType);
        } catch (RemoteVideoImportException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteVideoImportException("Failed to download remote video: " + e.getMessage(), e);
        }
    }

    private ResolvedVideoSource resolveVideoSource(URI sourceUri, int depth, URI referer) throws RemoteVideoImportException {
        if (depth > MAX_RESOLUTION_DEPTH) {
            throw new RemoteVideoImportException("Provider video URL resolution exceeded max redirect depth");
        }
        if (!requiresProviderResolution(sourceUri)) {
            return new ResolvedVideoSource(sourceUri, referer != null ? referer.toString() : null, null, sourceUri);
        }

        String html = fetchHtml(sourceUri);
        String resolvedUrl = extractVideoUrlFromHtml(sourceUri, html);
        if (resolvedUrl == null || resolvedUrl.isBlank()) {
            throw unresolvedSocialUrlException(sourceUri);
        }

        URI resolvedUri = validateUrl(resolvedUrl);
        if (requiresProviderResolution(resolvedUri)) {
            return resolveVideoSource(resolvedUri, depth + 1, sourceUri);
        }
        return new ResolvedVideoSource(
                resolvedUri,
                sourceUri.toString(),
                extractOgTitle(html),
                sourceUri);
    }

    private String fetchHtml(URI uri) throws RemoteVideoImportException {
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
                throw new RemoteVideoImportException("Remote server returned HTTP " + status);
            }
            return response.body();
        } catch (RemoteVideoImportException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteVideoImportException("Failed to resolve provider video page: " + e.getMessage(), e);
        }
    }

    private static URI validateUrl(String sourceUrl) throws RemoteVideoImportException {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new RemoteVideoImportException("Video URL is required");
        }
        try {
            URI uri = URI.create(sourceUrl.trim());
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new RemoteVideoImportException("Only http and https video URLs are supported");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new RemoteVideoImportException("Video URL must include a host");
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new RemoteVideoImportException("Invalid video URL", e);
        }
    }

    private static byte[] readLimited(InputStream inputStream, long maxBytes) throws Exception {
        try (inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new RemoteVideoImportException("Remote video exceeds max size of " + maxBytes + " bytes");
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

    private static String determineFilename(URI uri, String contentType, String contentDisposition, String filenameHint) {
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
                String raw = trimmed.substring("filename=".length()).replace("\"", "").trim();
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

    private static boolean requiresProviderResolution(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return isInstagramHost(host) || isFacebookHost(host) || isYouTubeHost(host);
    }

    private static boolean isInstagramHost(String host) {
        return host.contains("instagram.com");
    }

    private static boolean isFacebookHost(String host) {
        return host.contains("facebook.com") || host.contains("fb.watch");
    }

    private static boolean isYouTubeHost(String host) {
        return host.contains("youtube.com") || host.contains("youtu.be");
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
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("//");
    }

    private static String normalizeExtractedUrl(URI pageUri, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .replace("&amp;", "&")
                .replace("\\/", "/");
        normalized = decodeUnicodeEscapes(normalized);
        if (normalized.startsWith("//")) {
            String scheme = pageUri != null && pageUri.getScheme() != null ? pageUri.getScheme() : "https";
            return scheme + ":" + normalized;
        }
        return normalized;
    }

    private static String decodeUnicodeEscapes(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            if (i + 5 < value.length()
                    && value.charAt(i) == '\\'
                    && value.charAt(i + 1) == 'u') {
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

    private static RemoteVideoImportException socialUrlException(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (requiresProviderResolution(uri)) {
            return unresolvedSocialUrlException(uri);
        }
        return new RemoteVideoImportException("URL returned HTML instead of a video file");
    }

    private static RemoteVideoImportException unresolvedSocialUrlException(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (isInstagramHost(host) || isFacebookHost(host)) {
            return new RemoteVideoImportException(
                    "Could not resolve a downloadable Instagram or Facebook video from that page URL. The post may be private, geo-restricted, or require authentication.");
        }
        if (isYouTubeHost(host)) {
            return new RemoteVideoImportException(
                    "Could not resolve a downloadable YouTube video stream from that page URL. The video may require a provider-specific extractor or have stream protection enabled.");
        }
        return new RemoteVideoImportException("Could not resolve a downloadable video from that page URL");
    }

    private record ResolvedVideoSource(URI uri, String referer, String filenameHint, URI sourcePageUri) {}

    public record DownloadedVideo(byte[] bytes, String filename, String contentType) {}

    public static class RemoteVideoImportException extends Exception {
        public RemoteVideoImportException(String message) {
            super(message);
        }

        public RemoteVideoImportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
