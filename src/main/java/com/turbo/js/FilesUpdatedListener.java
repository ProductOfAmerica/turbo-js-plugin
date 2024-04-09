package com.turbo.js;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;

import java.util.ArrayList;

public interface FilesUpdatedListener {
    public static final Topic<FilesUpdatedListener> UPDATE_TOPIC = Topic.create(FilesUpdatedListener.class.getName(), FilesUpdatedListener.class);

    void onFilesUpdated(ArrayList<VirtualFile> files);
}