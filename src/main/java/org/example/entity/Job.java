package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
//если в JSON есть лишние поля, Jackson их проигнорирует
public class Job {
    private long id;
    private String date;
    private String company;
    private String position;
    private String location;
    private List<String> tags;
    private String description;
    private String slug;        // ← добавили
    private String url;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)\n%s\n", company, position, location, url);
    }
    //[%s]     → подставляется company (в квадратных скобках)
    //%s       → подставляется position (название вакансии)
    //(%s)     → подставляется location (в скобках)
    //\n       → перевод строки (новая строка)
    //%s       → подставляется url
    //\n       → ещё одна новая строка
}
