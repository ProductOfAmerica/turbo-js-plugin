package com.turbo.js;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;

import java.util.ArrayList;

public interface FilesLoadedListener {
    public static final Topic<FilesLoadedListener> LOADED_TOPIC = Topic.create(FilesLoadedListener.class.getName(), FilesLoadedListener.class);

    void onFilesLoaded(ArrayList<VirtualFile> files);
}