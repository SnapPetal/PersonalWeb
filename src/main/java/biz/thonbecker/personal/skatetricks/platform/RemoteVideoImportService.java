package biz.thonbecker.personal.skatetricks.platform;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RemoteVideoImportService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);
    private static final List<String> SUPPORTED_VIDEO_TYPES = List.of(
            "video/mp4", "video/quicktime", "video/webm", "video/x-msvideo", "video/x-matroska", "application/octet-stream");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public DownloadedVideo downloadVideo(String sourceUrl, long maxBytes) throws RemoteVideoImportException {
        URI uri = validateUrl(sourceUrl);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "PersonalWeb-Skatetricks/1.0")
                .GET()
                .build();

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
                throw socialUrlException(uri);
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
            String filename = determineFilename(uri, normalizedContentType, response.headers().firstValue("content-disposition").orElse(null));
            return new DownloadedVideo(bytes, filename, normalizedContentType);
        } catch (RemoteVideoImportException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteVideoImportException("Failed to download remote video: " + e.getMessage(), e);
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

    private static String determineFilename(URI uri, String contentType, String contentDisposition) {
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

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "video/quicktime" -> ".mov";
            case "video/webm" -> ".webm";
            case "video/x-msvideo" -> ".avi";
            case "video/x-matroska" -> ".mkv";
            default -> ".mp4";
        };
    }

    private static RemoteVideoImportException socialUrlException(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.contains("instagram.com") || host.contains("facebook.com") || host.contains("fb.watch")) {
            return new RemoteVideoImportException(
                    "Instagram and Facebook page URLs are not direct video files. Use a direct video URL or add provider-specific extraction.");
        }
        return new RemoteVideoImportException("URL returned HTML instead of a video file");
    }

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
