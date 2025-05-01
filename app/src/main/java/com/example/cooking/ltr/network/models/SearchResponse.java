package com.example.cooking.ltr.network.models;

import com.example.cooking.ltr.models.SearchResult;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Модель ответа сервера на поисковый запрос
 */
public class SearchResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("total_results")
    private int totalResults;

    @SerializedName("page")
    private int page;

    @SerializedName("per_page")
    private int perPage;

    @SerializedName("results")
    private List<SearchResult> results;

    @SerializedName("search_metadata")
    private SearchMetadata searchMetadata;

    /**
     * Геттеры и сеттеры
     */

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPerPage() {
        return perPage;
    }

    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    public List<SearchResult> getResults() {
        return results;
    }

    public void setResults(List<SearchResult> results) {
        this.results = results;
    }

    public SearchMetadata getSearchMetadata() {
        return searchMetadata;
    }

    public void setSearchMetadata(SearchMetadata searchMetadata) {
        this.searchMetadata = searchMetadata;
    }

    /**
     * Вложенный класс для метаданных поиска
     */
    public static class SearchMetadata {

        @SerializedName("query_analysis")
        private String queryAnalysis;

        @SerializedName("applied_filters")
        private List<String> appliedFilters;

        @SerializedName("suggestion")
        private String suggestion;

        public String getQueryAnalysis() {
            return queryAnalysis;
        }

        public void setQueryAnalysis(String queryAnalysis) {
            this.queryAnalysis = queryAnalysis;
        }

        public List<String> getAppliedFilters() {
            return appliedFilters;
        }

        public void setAppliedFilters(List<String> appliedFilters) {
            this.appliedFilters = appliedFilters;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }
}