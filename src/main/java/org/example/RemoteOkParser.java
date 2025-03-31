package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.example.entity.Job;

import java.util.List;

public class RemoteOkParser {
    private static final String API_URL = "https://remoteok.io/api";

    public static void main(String[] args) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(API_URL);
            request.setHeader("User-Agent", "Mozilla/5.0"); // иначе может быть 403

            ClassicHttpResponse response = (ClassicHttpResponse) client.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            //JSON -> List<Job>
            ObjectMapper mapper = new ObjectMapper();
            List<Job> allJobs = mapper.readValue(responseBody, new TypeReference<>() {});

            //пропускаем первую запись (мета-информация)
            List<Job> jobs = allJobs.subList(1, allJobs.size());

            //выводим вакансии, содержащие "java" (регистр не важен)
            for (Job job : jobs) {
                String title = job.getPosition();
                List<String> tags = job.getTags();

                if (title != null && title.toLowerCase().contains("java")) {
                    System.out.println(job);
                } else if (tags != null && tags.stream().anyMatch(t -> t.toLowerCase().contains("java"))) {
                    System.out.println(job);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
