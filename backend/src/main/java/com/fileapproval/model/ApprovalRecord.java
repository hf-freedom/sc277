package com.fileapproval.model;

import com.fileapproval.enums.ApprovalStatus;

import java.time.LocalDateTime;

public class ApprovalRecord {
    private String id;
    private String fileVersionId;
    private String approver;
    private String approvalChainName;
    private Integer approvalLevel;
    private ApprovalStatus status;
    private String comment;
    private LocalDateTime approvalTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileVersionId() { return fileVersionId; }
    public void setFileVersionId(String fileVersionId) { this.fileVersionId = fileVersionId; }
    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }
    public String getApprovalChainName() { return approvalChainName; }
    public void setApprovalChainName(String approvalChainName) { this.approvalChainName = approvalChainName; }
    public Integer getApprovalLevel() { return approvalLevel; }
    public void setApprovalLevel(Integer approvalLevel) { this.approvalLevel = approvalLevel; }
    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getApprovalTime() { return approvalTime; }
    public void setApprovalTime(LocalDateTime approvalTime) { this.approvalTime = approvalTime; }
}
