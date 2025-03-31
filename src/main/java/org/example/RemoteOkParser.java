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
    //современный logger вместо устаревшего негибкого e.printStackTrace();
    private static final Logger logger = LoggerFactory.getLogger(RemoteOkParser.class);

    //private static final Set<String> KEYWORDS = Set.of("java", "junior", "remote", "intern");

    public static void main(String[] args) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(API_URL);
            request.setHeader("User-Agent", "Mozilla/5.0"); // иначе может быть 403

            String responseBody = client.execute(request, response ->
                    EntityUtils.toString(response.getEntity())
            );

            //JSON -> List<Job>
            ObjectMapper mapper = new ObjectMapper();
            List<Job> allJobs = mapper.readValue(responseBody, new TypeReference<>() {});

            //пропускаем первую запись (мета-информация)
            List<Job> jobs = allJobs.subList(1, allJobs.size());

            //список для записи в CSV
            List<Job> matchedJobs = new ArrayList<>();

            Scanner scanner = new Scanner(System.in);
            System.out.print("Введите ключевые слова через пробел: ");
            String[] keywordInput = scanner.nextLine().toLowerCase().split("\\s+");
            Set<String> keywords = new HashSet<>(Arrays.asList(keywordInput));

            System.out.print("За сколько последних дней показывать вакансии? ");
            int days = Integer.parseInt(scanner.nextLine());

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

            //выводим вакансии, содержащие "java" (регистр не важен)
            for (Job job : jobs) {
                // Преобразуем дату из строки
                LocalDateTime jobDate = null;
                try {
                    jobDate = OffsetDateTime.parse(job.getDate(), formatter).toLocalDateTime();
                } catch (Exception ignored) {}

                if (jobDate == null || jobDate.isBefore(cutoffDate)) {
                    continue; // пропускаем старые вакансии
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

            //сохранение в CSV
            try (PrintWriter writer = new PrintWriter(new FileWriter("vacancies.csv"))) {
                writer.println("Company,Position,Location,URL");

                for (Job job : matchedJobs) {
                    //экранируем запятые и кавычки
                    String company = escapeCsv(job.getCompany());
                    String position = escapeCsv(job.getPosition());
                    String location = escapeCsv(job.getLocation());
                    String url = escapeCsv(job.getUrl());

                    writer.printf("%s,%s,%s,%s%n", company, position, location, url);
                }
            }
            System.out.println("\nJobs successfully saved to file vacancies.csv");

        } catch (Exception e) {
            logger.error("Ошибка при выполнении запроса или обработке данных", e);
        }
    }

    // Экранирование значений для CSV (кавычки и запятые)
    public static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }
}
