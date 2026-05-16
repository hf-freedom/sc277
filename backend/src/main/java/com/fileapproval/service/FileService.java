package com.fileapproval.service;

import com.fileapproval.config.ApprovalChainConfig;
import com.fileapproval.dto.Response;
import com.fileapproval.enums.ApprovalStatus;
import com.fileapproval.enums.FileStatus;
import com.fileapproval.enums.FileType;
import com.fileapproval.model.ApprovalRecord;
import com.fileapproval.model.FileInfo;
import com.fileapproval.model.FileVersion;
import com.fileapproval.storage.DataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileService {
    @Autowired
    private ApprovalChainConfig approvalChainConfig;

    private final String uploadPath = "./uploads";

    public Response<FileVersion> uploadFile(MultipartFile file, String description, String uploader, String fileType,
                                             Boolean isSensitive, Integer downloadLimit, Boolean requiresAdditionalReview) throws IOException {
        Files.createDirectories(Paths.get(uploadPath));

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(UUID.randomUUID().toString());
        fileInfo.setFileName(file.getOriginalFilename());
        if (fileType != null && !fileType.isEmpty()) {
            fileInfo.setType(FileType.valueOf(fileType));
        } else {
            fileInfo.setType(determineFileType(file.getOriginalFilename()));
        }
        fileInfo.setCreator(uploader);
        fileInfo.setCreateTime(LocalDateTime.now());
        fileInfo.setSensitive(isSensitive != null && isSensitive);
        fileInfo.setDownloadLimit(fileInfo.isSensitive() && downloadLimit != null ? downloadLimit : null);
        fileInfo.setRequiresAdditionalReview(isSensitive != null && isSensitive && requiresAdditionalReview != null && requiresAdditionalReview);

        FileVersion fileVersion = createNewVersion(fileInfo, file, description, uploader, isSensitive, downloadLimit, requiresAdditionalReview);

        DataStorage.getFileInfoMap().put(fileInfo.getId(), fileInfo);
        DataStorage.getFileVersionMap().put(fileVersion.getId(), fileVersion);

        return Response.success(fileVersion);
    }

    public Response<Map<String, List<String>>> getAllApprovalChains() {
        Map<String, List<String>> result = new java.util.HashMap<>();
        for (Map.Entry<FileType, List<String>> entry : approvalChainConfig.getAllApprovalChains().entrySet()) {
            result.put(entry.getKey().name(), entry.getValue());
        }
        return Response.success(result);
    }

    private FileVersion createNewVersion(FileInfo fileInfo, MultipartFile file, String description, String uploader,
                                           Boolean isSensitive, Integer downloadLimit, Boolean requiresAdditionalReview) throws IOException {
        int versionNumber = fileInfo.getVersionIds().size() + 1;
        String version = "v" + versionNumber;

        String fileExtension = getFileExtension(file.getOriginalFilename());
        String storedFileName = fileInfo.getId() + "_" + version + "." + fileExtension;
        Path filePath = Paths.get(uploadPath, storedFileName);
        Files.copy(file.getInputStream(), filePath);

        FileVersion fileVersion = new FileVersion();
        fileVersion.setId(UUID.randomUUID().toString());
        fileVersion.setFileId(fileInfo.getId());
        fileVersion.setFileName(file.getOriginalFilename());
        fileVersion.setFileType(fileExtension);
        fileVersion.setType(fileInfo.getType());
        fileVersion.setFileSize(file.getSize());
        fileVersion.setFilePath(filePath.toString());
        fileVersion.setVersion(version);
        fileVersion.setVersionNumber(versionNumber);
        fileVersion.setStatus(FileStatus.DRAFT);
        fileVersion.setUploader(uploader);
        fileVersion.setUploadTime(LocalDateTime.now());
        fileVersion.setDescription(description);
        fileVersion.setDraftExpireTime(LocalDateTime.now().plusDays(7));

        boolean sensitive = isSensitive != null && isSensitive;
        fileVersion.setIsSensitive(sensitive);
        if (sensitive) {
            fileVersion.setDownloadLimit(downloadLimit != null ? downloadLimit : 3);
            fileVersion.setRequiresAdditionalReview(requiresAdditionalReview != null && requiresAdditionalReview);
        }

        fileInfo.getVersionIds().add(fileVersion.getId());
        fileInfo.setCurrentDraftVersionId(fileVersion.getId());

        return fileVersion;
    }

    public Response<FileVersion> updateDraft(String versionId, MultipartFile file, String description) throws IOException {
        FileVersion version = DataStorage.getFileVersionMap().get(versionId);
        if (version == null) {
            return Response.error("版本不存在");
        }
        if (version.getStatus() != FileStatus.DRAFT && version.getStatus() != FileStatus.REJECTED) {
            return Response.error("只能修改草稿或被驳回的版本");
        }

        if (file != null) {
            Files.deleteIfExists(Paths.get(version.getFilePath()));
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String storedFileName = version.getFileId() + "_" + version.getVersion() + "." + fileExtension;
            Path filePath = Paths.get(uploadPath, storedFileName);
            Files.copy(file.getInputStream(), filePath);
            version.setFilePath(filePath.toString());
            version.setFileSize(file.getSize());
        }

        if (description != null) {
            version.setDescription(description);
        }
        version.setUploadTime(LocalDateTime.now());
        version.setDraftExpireTime(LocalDateTime.now().plusDays(7));

        return Response.success(version);
    }

    public Response<FileVersion> submitForApproval(String versionId) {
        FileVersion version = DataStorage.getFileVersionMap().get(versionId);
        if (version == null) {
            return Response.error("版本不存在");
        }
        if (version.getStatus() != FileStatus.DRAFT && version.getStatus() != FileStatus.REJECTED) {
            return Response.error("只能提交草稿或被驳回的版本");
        }

        List<String> approvalChain = approvalChainConfig.getApprovalChain(version.getType());
        version.setCurrentApprovalChain(approvalChain.get(0));
        version.setCurrentApprovalIndex(0);
        version.setStatus(FileStatus.PENDING_APPROVAL);

        ApprovalRecord firstRecord = new ApprovalRecord();
        firstRecord.setId(UUID.randomUUID().toString());
        firstRecord.setFileVersionId(versionId);
        firstRecord.setApprovalChainName(approvalChain.get(0));
        firstRecord.setApprovalLevel(0);
        firstRecord.setStatus(ApprovalStatus.PENDING);
        version.getApprovalRecords().add(firstRecord);

        return Response.success(version);
    }

    public Response<FileVersion> approve(String versionId, String approver, String comment, boolean isAdditionalReview) {
        FileVersion version = DataStorage.getFileVersionMap().get(versionId);
        if (version == null) {
            return Response.error("版本不存在");
        }
        if (version.getStatus() != FileStatus.PENDING_APPROVAL && version.getStatus() != FileStatus.APPROVING) {
            return Response.error("该版本不在审批中");
        }

        if (version.getApprovalRecords().isEmpty()) {
            return Response.error("审批记录不存在");
        }

        ApprovalRecord currentRecord = version.getApprovalRecords().get(version.getApprovalRecords().size() - 1);
        currentRecord.setApprover(approver);
        currentRecord.setComment(comment);
        currentRecord.setStatus(ApprovalStatus.APPROVED);
        currentRecord.setApprovalTime(LocalDateTime.now());

        if (isAdditionalReview && version.getRequiresAdditionalReview() && version.getCurrentApprovalIndex() >= approvalChainConfig.getApprovalChain(version.getType()).size() - 1) {
            version.setRequiresAdditionalReview(false);
            version.setStatus(FileStatus.APPROVED);
            return publish(versionId);
        }

        List<String> approvalChain = approvalChainConfig.getApprovalChain(version.getType());
        int nextIndex = version.getCurrentApprovalIndex() + 1;

        if (nextIndex < approvalChain.size()) {
            version.setCurrentApprovalIndex(nextIndex);
            version.setCurrentApprovalChain(approvalChain.get(nextIndex));
            version.setStatus(FileStatus.APPROVING);

            ApprovalRecord nextRecord = new ApprovalRecord();
            nextRecord.setId(UUID.randomUUID().toString());
            nextRecord.setFileVersionId(versionId);
            nextRecord.setApprovalChainName(approvalChain.get(nextIndex));
            nextRecord.setApprovalLevel(nextIndex);
            nextRecord.setStatus(ApprovalStatus.PENDING);
            version.getApprovalRecords().add(nextRecord);
        } else {
            version.setStatus(FileStatus.APPROVED);
            return publish(versionId);
        }

        return Response.success(version);
    }

    public Response<FileVersion> reject(String versionId, String approver, String comment) {
        FileVersion version = DataStorage.getFileVersionMap().get(versionId);
        if (version == null) {
            return Response.error("版本不存在");
        }
        if (version.getStatus() != FileStatus.PENDING_APPROVAL && version.getStatus() != FileStatus.APPROVING) {
            return Response.error("该版本不在审批中");
        }

        if (!version.getApprovalRecords().isEmpty()) {
            ApprovalRecord currentRecord = version.getApprovalRecords().get(version.getApprovalRecords().size() - 1);
            currentRecord.setApprover(approver);
            currentRecord.setComment(comment);
            currentRecord.setStatus(ApprovalStatus.REJECTED);
            currentRecord.setApprovalTime(LocalDateTime.now());
        }

        version.setStatus(FileStatus.REJECTED);
        version.setCurrentApprovalChain(null);
        version.setCurrentApprovalIndex(null);

        return Response.success(version);
    }

    private Response<FileVersion> publish(String versionId) {
        FileVersion version = DataStorage.getFileVersionMap().get(versionId);
        if (version == null) {
            return Response.error("版本不存在");
        }

        FileInfo fileInfo = DataStorage.getFileInfoMap().get(version.getFileId());
        if (fileInfo.getCurrentPublishedVersionId() != null) {
            FileVersion oldVersion = DataStorage.getFileVersionMap().get(fileInfo.getCurrentPublishedVersionId());
            if (oldVersion != null) {
                oldVersion.setStatus(FileStatus.ARCHIVED);
                fileInfo.getArchivedVersionIds().add(fileInfo.getCurrentPublishedVersionId());
            }
        }

        version.setStatus(FileStatus.PUBLISHED);
        version.setPublishTime(LocalDateTime.now());
        fileInfo.setCurrentPublishedVersionId(versionId);

        return Response.success(version);
    }

    public Response<String> withdraw(String versionId) {
        FileVersion version = DataStorage.getFileVersionMap().get(versionId);
        if (version == null) {
            return Response.error("版本不存在");
        }

        if (version.getReferencedBy() != null && !version.getReferencedBy().isEmpty()) {
            return Response.error("该文件已被其他流程引用，无法撤回");
        }

        FileInfo fileInfo = DataStorage.getFileInfoMap().get(version.getFileId());
        if (versionId.equals(fileInfo.getCurrentPublishedVersionId())) {
            fileInfo.setCurrentPublishedVersionId(null);
        }

        version.setStatus(FileStatus.WITHDRAWN);
        return Response.success("撤回成功");
    }

    public Response<byte[]> downloadFile(String versionId) throws IOException {
        FileVersion version = DataStorage.getFileVersionMap().get(versionId);
        if (version == null) {
            return Response.error("版本不存在");
        }

        if (version.getStatus() != FileStatus.PUBLISHED) {
            return Response.error("只能下载已发布的文件");
        }

        if (version.getIsSensitive() && version.getDownloadLimit() != null) {
            if (version.getDownloadCount() >= version.getDownloadLimit()) {
                return Response.error("下载次数已达上限");
            }
        }

        version.setDownloadCount(version.getDownloadCount() + 1);

        Path path = Paths.get(version.getFilePath());
        byte[] content = Files.readAllBytes(path);
        return Response.success(content);
    }

    public Response<FileVersion> rollbackPublish(String versionId) {
        FileVersion version = DataStorage.getFileVersionMap().get(versionId);
        if (version == null) {
            return Response.error("版本不存在");
        }

        FileInfo fileInfo = DataStorage.getFileInfoMap().get(version.getFileId());

        version.setStatus(FileStatus.APPROVED);

        if (!fileInfo.getArchivedVersionIds().isEmpty()) {
            String lastArchivedId = fileInfo.getArchivedVersionIds().get(fileInfo.getArchivedVersionIds().size() - 1);
            FileVersion lastArchived = DataStorage.getFileVersionMap().get(lastArchivedId);
            if (lastArchived != null) {
                lastArchived.setStatus(FileStatus.PUBLISHED);
                fileInfo.setCurrentPublishedVersionId(lastArchivedId);
                fileInfo.getArchivedVersionIds().remove(lastArchivedId);
            }
        } else {
            fileInfo.setCurrentPublishedVersionId(null);
        }

        version.setCurrentApprovalChain(approvalChainConfig.getApprovalChain(version.getType()).get(0));
        version.setCurrentApprovalIndex(0);

        return Response.success(version);
    }

    public Response<List<FileInfo>> getAllFiles() {
        return Response.success(new java.util.ArrayList<>(DataStorage.getFileInfoMap().values()));
    }

    public Response<List<FileVersion>> getVersionsByFileId(String fileId) {
        FileInfo fileInfo = DataStorage.getFileInfoMap().get(fileId);
        if (fileInfo == null) {
            return Response.error("文件不存在");
        }

        List<FileVersion> versions = new java.util.ArrayList<>();
        for (String versionId : fileInfo.getVersionIds()) {
            FileVersion version = DataStorage.getFileVersionMap().get(versionId);
            if (version != null) {
                versions.add(version);
            }
        }
        versions.sort((a, b) -> b.getVersionNumber() - a.getVersionNumber());
        return Response.success(versions);
    }

    public Response<FileVersion> getVersion(String versionId) {
        FileVersion version = DataStorage.getFileVersionMap().get(versionId);
        if (version == null) {
            return Response.error("版本不存在");
        }
        return Response.success(version);
    }

    private FileType determineFileType(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();
        if (ext.matches("doc|docx|pdf|txt|xls|xlsx|ppt|pptx")) {
            return FileType.DOCUMENT;
        } else if (ext.matches("jpg|jpeg|png|gif|bmp|svg")) {
            return FileType.IMAGE;
        } else if (ext.matches("mp4|avi|mov|mkv|flv")) {
            return FileType.VIDEO;
        } else if (ext.matches("java|js|py|cpp|c|h|go|rs|ts|html|css")) {
            return FileType.CODE;
        } else if (ext.matches("yaml|yml|json|properties|xml|conf|cfg")) {
            return FileType.CONFIG;
        } else if (ext.matches("key|pem|p12|pfx|sec")) {
            return FileType.SENSITIVE;
        }
        return FileType.DOCUMENT;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    public void addReference(String versionId, String processId) {
        FileVersion version = DataStorage.getFileVersionMap().get(versionId);
        if (version != null && !version.getReferencedBy().contains(processId)) {
            version.getReferencedBy().add(processId);
        }
    }

    public void removeReference(String versionId, String processId) {
        FileVersion version = DataStorage.getFileVersionMap().get(versionId);
        if (version != null) {
            version.getReferencedBy().remove(processId);
        }
    }

    public Map<String, List<String>> getAllReferences() {
        Map<String, List<String>> references = new java.util.HashMap<>();
        for (Map.Entry<String, FileVersion> entry : DataStorage.getFileVersionMap().entrySet()) {
            if (entry.getValue().getReferencedBy() != null && !entry.getValue().getReferencedBy().isEmpty()) {
                references.put(entry.getKey(), entry.getValue().getReferencedBy());
            }
        }
        return references;
    }
}
