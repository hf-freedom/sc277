package com.fileapproval.storage;

import com.fileapproval.model.FileInfo;
import com.fileapproval.model.FileVersion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStorage {
    private static final Map<String, FileInfo> fileInfoMap = new ConcurrentHashMap<>();
    private static final Map<String, FileVersion> fileVersionMap = new ConcurrentHashMap<>();

    public static Map<String, FileInfo> getFileInfoMap() {
        return fileInfoMap;
    }

    public static Map<String, FileVersion> getFileVersionMap() {
        return fileVersionMap;
    }
}
