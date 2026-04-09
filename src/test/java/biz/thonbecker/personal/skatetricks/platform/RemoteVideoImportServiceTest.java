package biz.thonbecker.personal.skatetricks.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import org.junit.jupiter.api.Test;

class RemoteVideoImportServiceTest {

    @Test
    void extractsInstagramPlayableUrlFromEmbeddedJson() {
        String html = """
                <html>
                  <head>
                    <meta property="og:title" content="Crooked grind line" />
                  </head>
                  <body>
                    <script>
                      window.__additionalDataLoaded("/reel/test/", {"items":[{"video_versions":[{"type":101,"url":"https:\\/\\/scontent.cdninstagram.com\\/v\\/t50.2886-16\\/12345_n.mp4?_nc_cat=1\\u0026_nc_sid=8ae9d6"}]}]});
                    </script>
                  </body>
                </html>
                """;

        String resolved = RemoteVideoImportService.extractVideoUrlFromHtml(
                URI.create("https://www.instagram.com/reel/test/"), html);

        assertEquals(
                "https://scontent.cdninstagram.com/v/t50.2886-16/12345_n.mp4?_nc_cat=1&_nc_sid=8ae9d6",
                resolved);
    }

    @Test
    void extractsFacebookPlayableUrlFromPageJson() {
        String html = """
                <html>
                  <body>
                    <script>
                      {"playable_url_quality_hd":"https:\\/\\/video.xx.fbcdn.net\\/v\\/t42.1790-2\\/98765.mp4?strext=1\\u0026efg=foo"}
                    </script>
                  </body>
                </html>
                """;

        String resolved = RemoteVideoImportService.extractVideoUrlFromHtml(
                URI.create("https://www.facebook.com/watch/?v=123"), html);

        assertEquals(
                "https://video.xx.fbcdn.net/v/t42.1790-2/98765.mp4?strext=1&efg=foo",
                resolved);
    }

    @Test
    void extractsYouTubeMp4StreamFromPlayerResponse() {
        String html = """
                <html>
                  <body>
                    <script>
                      var ytInitialPlayerResponse = {"streamingData":{"formats":[{"itag":18,"mimeType":"video/mp4; codecs=\\"avc1.42001E, mp4a.40.2\\"","url":"https:\\/\\/rr1---sn-a5mekn7r.googlevideo.com\\/videoplayback?id=o-abc123\\u0026itag=18\\u0026source=youtube"}]}};
                    </script>
                  </body>
                </html>
                """;

        String resolved = RemoteVideoImportService.extractVideoUrlFromHtml(
                URI.create("https://www.youtube.com/watch?v=abc123"), html);

        assertEquals(
                "https://rr1---sn-a5mekn7r.googlevideo.com/videoplayback?id=o-abc123&itag=18&source=youtube",
                resolved);
    }

    @Test
    void fallsBackToOgVideoMetaTag() {
        String html = """
                <html>
                  <head>
                    <meta content="https://cdn.example.com/skate/session.mp4" property="og:video:secure_url">
                  </head>
                </html>
                """;

        String resolved = RemoteVideoImportService.extractVideoUrlFromHtml(
                URI.create("https://example.com/watch/123"), html);

        assertEquals("https://cdn.example.com/skate/session.mp4", resolved);
    }

    @Test
    void returnsNullWhenPageDoesNotContainDownloadableVideo() {
        String html = """
                <html>
                  <head><title>No video here</title></head>
                  <body><p>Nothing usable</p></body>
                </html>
                """;

        String resolved = RemoteVideoImportService.extractVideoUrlFromHtml(
                URI.create("https://www.youtube.com/watch?v=missing"), html);

        assertNull(resolved);
    }
}
