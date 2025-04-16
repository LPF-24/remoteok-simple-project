package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.example.entity.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RemoteOkParser {
    private static final String API_URL = "https://remoteok.io/api";
    // modern logger instead of deprecated inflexible e.printStackTrace();
    private static final Logger logger = LoggerFactory.getLogger(RemoteOkParser.class);

    //private static final Set<String> KEYWORDS = Set.of("java", "junior", "remote", "intern");

    public static void main(String[] args) {
        // Create an HTTP client in try-with-resources to automatically close the connection after execution.
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(API_URL); // Create a GET request to the specified API address.
            request.setHeader("User-Agent", "Mozilla/5.0"); // If you don't add it, you may get a response with code 403

            // Send the request and receive the response body (JSON) as a string. Use the modern lambda version of the execute method.
            String responseBody = client.execute(request, response ->
                    EntityUtils.toString(response.getEntity())
            );

            //JSON -> List<Job>
            ObjectMapper mapper = new ObjectMapper(); // Create a Jackson object for parsing JSON.
            List<Job> allJobs = mapper.readValue(responseBody, new TypeReference<>() {}); // Convert the JSON into a list of Job objects.

            List<Job> jobs = allJobs.subList(1, allJobs.size()); // Skip the first entry (meta information)

            List<Job> matchedJobs = new ArrayList<>(); // List to write to CSV

            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter keywords separated by spaces: ");
            // Split the string into an array of words, convert to lowercase, and save in a Set for filtering.
            String[] keywordInput = scanner.nextLine().toLowerCase().split("\\s+");
            Set<String> keywords = new HashSet<>(Arrays.asList(keywordInput));

            System.out.print("How many recent days should vacancies be shown? ");
            int days = Integer.parseInt(scanner.nextLine());

            // Calculate the "last permissible time" for posting a vacancy. Anything earlier is skipped.
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

            // Display vacancies containing "java" (case does not matter).
            for (Job job : jobs) {
                // Convert date from string
                LocalDateTime jobDate = null;
                try {
                    // Convert date from string to time object. If there is an error, skip it.
                    jobDate = OffsetDateTime.parse(job.getDate(), formatter).toLocalDateTime();
                } catch (Exception ignored) {}

                // If the publication date is before "last permissible time", skip the vacancy.
                if (jobDate == null || jobDate.isBefore(cutoffDate)) {
                    continue;
                }

                String title = Optional.ofNullable(job.getPosition()).orElse("").toLowerCase();
                String company = Optional.ofNullable(job.getCompany()).orElse("").toLowerCase();
                String description = Optional.ofNullable(job.getDescription()).orElse("").toLowerCase();
                String slug = Optional.ofNullable(job.getSlug()).orElse("").toLowerCase();
                List<String> tags = Optional.ofNullable(job.getTags()).orElse(Collections.emptyList());

                boolean matches = keywords.stream().anyMatch(keyword ->
                        title.contains(keyword)
                                || company.contains(keyword)
                                || description.contains(keyword)
                                || slug.contains(keyword)
                                || tags.stream().anyMatch(t -> t.toLowerCase().contains(keyword))
                );

                if (matches) {
                    matchedJobs.add(job);
                    System.out.println(job);
                }
            }

            // Saving to CSV.
            // Open the CSV file for writing. Write the column headers in the first line.
            try (PrintWriter writer = new PrintWriter(new FileWriter("vacancies.csv"))) {
                writer.println("Company,Position,Location,URL");

                for (Job job : matchedJobs) {
                    // We write all vacancies line by line, escaping quotes/commas so that the CSV is correct.
                    String company = escapeCsv(job.getCompany());
                    String position = escapeCsv(job.getPosition());
                    String location = escapeCsv(job.getLocation());
                    String url = escapeCsv(job.getUrl());

                    writer.printf("%s,%s,%s,%s%n", company, position, location, url);
                }
            }
            System.out.println("\nJobs successfully saved to file vacancies.csv");

        } catch (Exception e) {
            // Catch any exceptions and output them to the log using SLF4J.
            logger.error("An error occurred while executing the request or processing the data", e);
        }
    }

    // Escaping values for CSV (quotes and commas)
    public static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }
}
