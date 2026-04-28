package com.sonicres.demo.features.audio.fingerprint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ArcCloudClient {

    private static final Logger log = LoggerFactory.getLogger(ArcCloudClient.class);
    private final ObjectMapper objectMapper;

    @Value("${acrcloud.access.key}")
    private String accessKey;

    @Value("${acrcloud.access.secret}")
    private String accessSecret;

    @Value("${acrcloud.host}")
    private String host;

    public ArcCloudClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FingerprintResult identify(File audioFile) throws Exception {
        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
        return identify(audioBytes);
    }

    public FingerprintResult identify(byte[] audioBytes) throws Exception {
        String httpMethod = "POST";
        String httpUri = "/v1/identify";
        String dataType = "audio";
        String signatureVersion = "1";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        String toSign = httpMethod + "\n" + httpUri + "\n" + accessKey + "\n"
                + dataType + "\n" + signatureVersion + "\n" + timestamp;

        String signature = sign(toSign, accessSecret);

        // Build multipart request
        String boundary = "----AcrCloudBoundary" + System.currentTimeMillis();
        byte[] body = buildMultipartBody(boundary, audioBytes, accessKey, signature,
                timestamp, dataType, signatureVersion);

        URL url = new URL("https://" + host + httpUri);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty("Content-Length", String.valueOf(body.length));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        String response;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            response = br.lines().collect(Collectors.joining("\n"));
        }

        log.info("ACRCloud response: {}", response);
        return parseResponse(response);
    }

    private String sign(String toSign, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] bytes = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(bytes);
    }

    private byte[] buildMultipartBody(String boundary, byte[] audioBytes,
                                      String accessKey, String signature, String timestamp,
                                      String dataType, String signatureVersion) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String crlf = "\r\n";
        String dd = "--";

        // Helper to add text fields
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("access_key", accessKey);
        fields.put("sample_bytes", String.valueOf(audioBytes.length));
        fields.put("timestamp", timestamp);
        fields.put("signature", signature);
        fields.put("data_type", dataType);
        fields.put("signature_version", signatureVersion);

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            out.write((dd + boundary + crlf).getBytes());
            out.write(("Content-Disposition: form-data; name=\""
                    + entry.getKey() + "\"" + crlf + crlf).getBytes());
            out.write(entry.getValue().getBytes());
            out.write(crlf.getBytes());
        }

        // Audio file field
        out.write((dd + boundary + crlf).getBytes());
        out.write(("Content-Disposition: form-data; name=\"sample\"; filename=\"sample.wav\""
                + crlf).getBytes());
        out.write(("Content-Type: audio/wav" + crlf + crlf).getBytes());
        out.write(audioBytes);
        out.write(crlf.getBytes());
        out.write((dd + boundary + dd + crlf).getBytes());

        return out.toByteArray();
    }

    private FingerprintResult parseResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        int code = root.path("status").path("code").asInt();

        if (code != 0) {
            log.warn("ACRCloud no match or error. Code: {}", code);
            return new FingerprintResult(); // no match
        }

        JsonNode music = root.path("metadata").path("music").get(0);
        if (music == null) return new FingerprintResult();

        String title = music.path("title").asText();
        String artist = music.path("artists").get(0).path("name").asText();
        double score = music.path("score").asDouble() / 100.0;

        log.info("✅ ACRCloud match: {} - {} (confidence: {})", artist, title, score);

        JsonNode spotify = music.path("external_metadata").path("spotify");
        JsonNode deezer = music.path("external_metadata").path("deezer");

        FingerprintResult result = new FingerprintResult(title, artist, score);
        result.setAlbum(music.path("album").path("name").asText(null));
        result.setReleaseDate(music.path("release_date").asText(null));
        result.setDurationMs(music.path("duration_ms").asInt(0));
        result.setLabel(music.path("label").asText(null));
        result.setSpotifyTrackId(spotify.path("track").path("id").asText(null));
        result.setDeezerTrackId(deezer.path("track").path("id").asText(null));
        result.setCoverArtUrl(fetchDeezerCoverArt(result.getDeezerTrackId()));
        return result;
    }

    private String fetchDeezerCoverArt(String deezerTrackId) {
        if (deezerTrackId == null) return null;
        try {
            URL url = new URL("https://api.deezer.com/track/" + deezerTrackId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            String response;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                response = br.lines().collect(Collectors.joining("\n"));
            }

            JsonNode root = objectMapper.readTree(response);
            return root.path("album").path("cover_big").asText(null);

        } catch (Exception e) {
            log.warn("Failed to fetch Deezer cover art: {}", e.getMessage());
            return null;
        }
    }
}