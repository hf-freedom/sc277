package com.fileapproval.controller;

import com.fileapproval.dto.Response;
import com.fileapproval.model.FileInfo;
import com.fileapproval.model.FileVersion;
import com.fileapproval.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {
    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public Response<FileVersion> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "uploader", defaultValue = "匿名用户") String uploader,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "isSensitive", defaultValue = "false") Boolean isSensitive,
            @RequestParam(value = "downloadLimit", required = false) Integer downloadLimit,
            @RequestParam(value = "requiresAdditionalReview", defaultValue = "false") Boolean requiresAdditionalReview) throws IOException {
        return fileService.uploadFile(file, description, uploader, fileType, isSensitive, downloadLimit, requiresAdditionalReview);
    }

    @GetMapping("/approval-chains")
    public Response<Map<String, List<String>>> getAllApprovalChains() {
        return fileService.getAllApprovalChains();
    }

    @PutMapping("/draft/{versionId}")
    public Response<FileVersion> updateDraft(
            @PathVariable String versionId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "description", required = false) String description) throws IOException {
        return fileService.updateDraft(versionId, file, description);
    }

    @PostMapping("/submit/{versionId}")
    public Response<FileVersion> submitForApproval(@PathVariable String versionId) {
        return fileService.submitForApproval(versionId);
    }

    @PostMapping("/approve/{versionId}")
    public Response<FileVersion> approve(
            @PathVariable String versionId,
            @RequestParam("approver") String approver,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam(value = "isAdditionalReview", defaultValue = "false") boolean isAdditionalReview) {
        return fileService.approve(versionId, approver, comment, isAdditionalReview);
    }

    @PostMapping("/reject/{versionId}")
    public Response<FileVersion> reject(
            @PathVariable String versionId,
            @RequestParam("approver") String approver,
            @RequestParam(value = "comment", required = false) String comment) {
        return fileService.reject(versionId, approver, comment);
    }

    @PostMapping("/withdraw/{versionId}")
    public Response<String> withdraw(@PathVariable String versionId) {
        return fileService.withdraw(versionId);
    }

    @PostMapping("/rollback/{versionId}")
    public Response<FileVersion> rollbackPublish(@PathVariable String versionId) {
        return fileService.rollbackPublish(versionId);
    }

    @GetMapping("/download/{versionId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String versionId) throws IOException {
        Response<byte[]> response = fileService.downloadFile(versionId);
        if (response.getCode() != 200) {
            return ResponseEntity.badRequest().body(null);
        }

        FileVersion version = fileService.getVersion(versionId).getData();
        String fileName = URLEncoder.encode(version.getFileName(), StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(response.getData());
    }

    @GetMapping
    public Response<List<FileInfo>> getAllFiles() {
        return fileService.getAllFiles();
    }

    @GetMapping("/{fileId}/versions")
    public Response<List<FileVersion>> getVersionsByFileId(@PathVariable String fileId) {
        return fileService.getVersionsByFileId(fileId);
    }

    @GetMapping("/version/{versionId}")
    public Response<FileVersion> getVersion(@PathVariable String versionId) {
        return fileService.getVersion(versionId);
    }

    @GetMapping("/references")
    public Response<Map<String, List<String>>> getAllReferences() {
        return Response.success(fileService.getAllReferences());
    }
}
