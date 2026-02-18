import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BackendClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String baseUrl = "http://localhost:8080";

    public String createEntryTicket(Car car) throws Exception {
        String json = """
            {"plate":"%s","type":"%s"}
            """.formatted(escape(car.getPlate()), escape(car.getType()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tickets/entry"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Backend error: " + response.statusCode() + " -> " + response.body());
        }
        return response.body();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
