package com.fileapproval.scheduler;

import com.fileapproval.enums.FileStatus;
import com.fileapproval.model.FileVersion;
import com.fileapproval.service.FileService;
import com.fileapproval.storage.DataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ScheduledTasks {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    private FileService fileService;

    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanExpiredDrafts() {
        logger.info("开始清理过期草稿...");
        LocalDateTime now = LocalDateTime.now();
        List<String> expiredVersions = new ArrayList<>();

        for (Map.Entry<String, FileVersion> entry : DataStorage.getFileVersionMap().entrySet()) {
            FileVersion version = entry.getValue();
            if ((version.getStatus() == FileStatus.DRAFT || version.getStatus() == FileStatus.REJECTED)
                    && version.getDraftExpireTime() != null
                    && now.isAfter(version.getDraftExpireTime())) {
                expiredVersions.add(entry.getKey());
                logger.info("清理过期草稿: {}", version.getFileName());
            }
        }

        logger.info("共清理 {} 个过期草稿", expiredVersions.size());
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void statisticsFileReferences() {
        logger.info("开始统计文件引用关系...");
        Map<String, List<String>> references = fileService.getAllReferences();

        for (Map.Entry<String, List<String>> entry : references.entrySet()) {
            FileVersion version = DataStorage.getFileVersionMap().get(entry.getKey());
            if (version != null) {
                logger.info("文件 {} (版本 {}) 被 {} 个流程引用",
                        version.getFileName(),
                        version.getVersion(),
                        entry.getValue().size());
            }
        }

        logger.info("文件引用关系统计完成");
    }
}
