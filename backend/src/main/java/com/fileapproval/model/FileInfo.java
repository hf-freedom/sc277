package com.fileapproval.model;

import com.fileapproval.enums.FileType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileInfo {
    private String id;
    private String fileName;
    private FileType type;
    private String creator;
    private LocalDateTime createTime;
    private String currentPublishedVersionId;
    private String currentDraftVersionId;
    private List<String> versionIds;
    private List<String> archivedVersionIds;
    private boolean isSensitive;
    private Integer downloadLimit;
    private int downloadCount;
    private boolean requiresAdditionalReview;

    public FileInfo() {
        this.versionIds = new ArrayList<>();
        this.archivedVersionIds = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public FileType getType() { return type; }
    public void setType(FileType type) { this.type = type; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getCurrentPublishedVersionId() { return currentPublishedVersionId; }
    public void setCurrentPublishedVersionId(String currentPublishedVersionId) { this.currentPublishedVersionId = currentPublishedVersionId; }
    public String getCurrentDraftVersionId() { return currentDraftVersionId; }
    public void setCurrentDraftVersionId(String currentDraftVersionId) { this.currentDraftVersionId = currentDraftVersionId; }
    public List<String> getVersionIds() { return versionIds; }
    public void setVersionIds(List<String> versionIds) { this.versionIds = versionIds; }
    public List<String> getArchivedVersionIds() { return archivedVersionIds; }
    public void setArchivedVersionIds(List<String> archivedVersionIds) { this.archivedVersionIds = archivedVersionIds; }
    public boolean isSensitive() { return isSensitive; }
    public void setSensitive(boolean sensitive) { isSensitive = sensitive; }
    public Integer getDownloadLimit() { return downloadLimit; }
    public void setDownloadLimit(Integer downloadLimit) { this.downloadLimit = downloadLimit; }
    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }
    public boolean isRequiresAdditionalReview() { return requiresAdditionalReview; }
    public void setRequiresAdditionalReview(boolean requiresAdditionalReview) { this.requiresAdditionalReview = requiresAdditionalReview; }
}
