package dev.fernandotorres.catalogo.api;

public enum JobSortField {
    ID("id"),
    PUBLISHED_AT("publishedAt"),
    TITLE("title"),
    COMPANY("company"),
    SALARY_MIN("salaryMin");

    private final String property;

    JobSortField(String property) {
        this.property = property;
    }

    public String property() {
        return property;
    }
}
