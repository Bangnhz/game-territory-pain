package com.territorypaint.backend.dto;

import java.util.List;

/**
 * Leaderboard response DTO.
 * Requirements: 10.8, 10.9
 */
public class LeaderboardResponse {
    
    private List<LeaderboardEntry> entries;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private LeaderboardEntry currentUserEntry;
    
    public LeaderboardResponse() {
    }
    
    public LeaderboardResponse(List<LeaderboardEntry> entries, Integer page, Integer size, 
                              Long totalElements, Integer totalPages) {
        this(entries, page, size, totalElements, totalPages, null);
    }

    public LeaderboardResponse(List<LeaderboardEntry> entries, Integer page, Integer size, 
                              Long totalElements, Integer totalPages, LeaderboardEntry currentUserEntry) {
        this.entries = entries;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentUserEntry = currentUserEntry;
    }
    
    public List<LeaderboardEntry> getEntries() {
        return entries;
    }

    // Alias for frontend compatibility
    public List<LeaderboardEntry> getContent() {
        return entries;
    }
    
    public void setEntries(List<LeaderboardEntry> entries) {
        this.entries = entries;
    }
    
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = page;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size;
    }
    
    public Long getTotalElements() {
        return totalElements;
    }
    
    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }
    
    public Integer getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public LeaderboardEntry getCurrentUserEntry() {
        return currentUserEntry;
    }

    public void setCurrentUserEntry(LeaderboardEntry currentUserEntry) {
        this.currentUserEntry = currentUserEntry;
    }
}
