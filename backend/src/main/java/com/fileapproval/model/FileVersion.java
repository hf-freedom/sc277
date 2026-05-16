package com.fileapproval.model;

import com.fileapproval.enums.FileStatus;
import com.fileapproval.enums.FileType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileVersion {
    private String id;
    private String fileId;
    private String fileName;
    private String fileType;
    private FileType type;
    private Long fileSize;
    private String filePath;
    private String version;
    private Integer versionNumber;
    private FileStatus status;
    private String uploader;
    private LocalDateTime uploadTime;
    private LocalDateTime publishTime;
    private String description;
    private Boolean isSensitive;
    private Integer downloadLimit;
    private Integer downloadCount;
    private List<String> referencedBy;
    private List<ApprovalRecord> approvalRecords;
    private String currentApprovalChain;
    private Integer currentApprovalIndex;
    private Boolean requiresAdditionalReview;
    private LocalDateTime draftExpireTime;

    public FileVersion() {
        this.downloadCount = 0;
        this.referencedBy = new ArrayList<>();
        this.approvalRecords = new ArrayList<>();
        this.requiresAdditionalReview = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public FileType getType() { return type; }
    public void setType(FileType type) { this.type = type; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
    public FileStatus getStatus() { return status; }
    public void setStatus(FileStatus status) { this.status = status; }
    public String getUploader() { return uploader; }
    public void setUploader(String uploader) { this.uploader = uploader; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsSensitive() { return isSensitive; }
    public void setIsSensitive(Boolean isSensitive) { this.isSensitive = isSensitive; }
    public Integer getDownloadLimit() { return downloadLimit; }
    public void setDownloadLimit(Integer downloadLimit) { this.downloadLimit = downloadLimit; }
    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }
    public List<String> getReferencedBy() { return referencedBy; }
    public void setReferencedBy(List<String> referencedBy) { this.referencedBy = referencedBy; }
    public List<ApprovalRecord> getApprovalRecords() { return approvalRecords; }
    public void setApprovalRecords(List<ApprovalRecord> approvalRecords) { this.approvalRecords = approvalRecords; }
    public String getCurrentApprovalChain() { return currentApprovalChain; }
    public void setCurrentApprovalChain(String currentApprovalChain) { this.currentApprovalChain = currentApprovalChain; }
    public Integer getCurrentApprovalIndex() { return currentApprovalIndex; }
    public void setCurrentApprovalIndex(Integer currentApprovalIndex) { this.currentApprovalIndex = currentApprovalIndex; }
    public Boolean getRequiresAdditionalReview() { return requiresAdditionalReview; }
    public void setRequiresAdditionalReview(Boolean requiresAdditionalReview) { this.requiresAdditionalReview = requiresAdditionalReview; }
    public LocalDateTime getDraftExpireTime() { return draftExpireTime; }
    public void setDraftExpireTime(LocalDateTime draftExpireTime) { this.draftExpireTime = draftExpireTime; }
}
