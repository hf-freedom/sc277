package com.fileapproval.config;

import com.fileapproval.enums.FileType;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ApprovalChainConfig {
    private final Map<FileType, List<String>> approvalChains = new HashMap<>();

    public ApprovalChainConfig() {
        approvalChains.put(FileType.DOCUMENT, Arrays.asList("部门主管", "文档管理员"));
        approvalChains.put(FileType.IMAGE, Arrays.asList("设计主管", "内容审核"));
        approvalChains.put(FileType.VIDEO, Arrays.asList("内容审核", "运营主管", "总监"));
        approvalChains.put(FileType.CODE, Arrays.asList("代码评审", "技术主管"));
        approvalChains.put(FileType.CONFIG, Arrays.asList("运维工程师", "技术主管"));
        approvalChains.put(FileType.SENSITIVE, Arrays.asList("部门主管", "安全官", "总监"));
    }

    public List<String> getApprovalChain(FileType fileType) {
        return approvalChains.getOrDefault(fileType, Collections.singletonList("管理员"));
    }

    public boolean isSensitiveType(FileType fileType) {
        return fileType == FileType.SENSITIVE;
    }

    public Integer getDownloadLimit(FileType fileType) {
        if (fileType == FileType.SENSITIVE) {
            return 3;
        }
        return null;
    }

    public Map<FileType, List<String>> getAllApprovalChains() {
        return approvalChains;
    }
}
