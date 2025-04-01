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
        //Создаём HTTP-клиент в try-with-resources, чтобы автоматически закрыть соединение после выполнения
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(API_URL); //Создаём GET-запрос на указанный API-адрес.
            request.setHeader("User-Agent", "Mozilla/5.0"); // иначе может быть 403

            //Отправляем запрос и получаем тело ответа (JSON) как строку. Используем современную лямбда-версию метода execute
            String responseBody = client.execute(request, response ->
                    EntityUtils.toString(response.getEntity())
            );

            //JSON -> List<Job>
            ObjectMapper mapper = new ObjectMapper(); //Создаём объект Jackson для парсинга JSON.
            List<Job> allJobs = mapper.readValue(responseBody, new TypeReference<>() {}); //Преобразуем JSON в список объектов Job

            List<Job> jobs = allJobs.subList(1, allJobs.size()); //пропускаем первую запись (мета-информация)

            List<Job> matchedJobs = new ArrayList<>(); //список для записи в CSV

            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter keywords separated by spaces: ");
            //Разбиваем строку на массив слов, приводим к нижнему регистру и сохраняем в Set для фильтрации
            String[] keywordInput = scanner.nextLine().toLowerCase().split("\\s+");
            Set<String> keywords = new HashSet<>(Arrays.asList(keywordInput));

            System.out.print("How many recent days should vacancies be shown? ");
            int days = Integer.parseInt(scanner.nextLine());

            //Вычисляем "крайнее допустимое время" публикации вакансии. Всё, что раньше — пропускаем
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

            //выводим вакансии, содержащие "java" (регистр не важен)
            for (Job job : jobs) {
                // Преобразуем дату из строки
                LocalDateTime jobDate = null;
                try {
                    //Преобразуем дату из строки в объект времени. Если ошибка — пропускаем
                    jobDate = OffsetDateTime.parse(job.getDate(), formatter).toLocalDateTime();
                } catch (Exception ignored) {}

                //Если дата публикации старая — пропускаем вакансию
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
            //Открываем CSV-файл на запись. В первую строку пишем заголовки колонок.
            try (PrintWriter writer = new PrintWriter(new FileWriter("vacancies.csv"))) {
                writer.println("Company,Position,Location,URL");

                for (Job job : matchedJobs) {
                    //Пишем все вакансии построчно, экранируя кавычки/запятые, чтобы CSV был корректным
                    String company = escapeCsv(job.getCompany());
                    String position = escapeCsv(job.getPosition());
                    String location = escapeCsv(job.getLocation());
                    String url = escapeCsv(job.getUrl());

                    writer.printf("%s,%s,%s,%s%n", company, position, location, url);
                }
            }
            System.out.println("\nJobs successfully saved to file vacancies.csv");

        } catch (Exception e) {
            //Ловим любые исключения, выводим в лог с помощью SLF4J.
            logger.error("An error occurred while executing the request or processing the data", e);
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
