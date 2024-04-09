package com.turbo.js.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.turbo.js.FilesUpdatedListener.UPDATE_TOPIC;

public class TurboJsonFileListener implements AsyncFileListener {
    private final Project project;

    public TurboJsonFileListener(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public ChangeApplier prepareChange(List<? extends VFileEvent> events) {
        ArrayList<VirtualFile> files = new ArrayList<>();

        for (VFileEvent event : events) {
            VirtualFile file = Objects.requireNonNull(event.getFile());
            if (file.getName().equals("turbo.json")) {
                System.out.println("adding turbo.json in async TurboJsonFileListener");
                files.add(file);
            }
        }

        // group by file type, use directory structure of grouped files to build hierarchy class.
        // you need to define the class.
        // do all of this.

        return !files.isEmpty() ? new ChangeApplier() {
            @Override
            public void afterVfsChange() {
                project.getMessageBus().syncPublisher(UPDATE_TOPIC).onFilesUpdated(files);
            }
        } : null;
    }
}
