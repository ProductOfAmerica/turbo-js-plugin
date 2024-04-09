package com.turbo.js.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.turbo.js.FileEventListener;
import com.turbo.js.FilesUpdatedListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class FilesUpdatedListenerImpl extends FileEventListener implements FilesUpdatedListener {
    public FilesUpdatedListenerImpl(Project project, ToolWindow toolWindow, Disposable parentDisposer) {
        super(project, toolWindow, parentDisposer);
    }

    @Override
    public void onFilesUpdated(ArrayList<VirtualFile> files) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Updating turbo files", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                System.out.println("Turbo files updated!");

                for (VirtualFile file : files) {
                    if (indicator.isCanceled()) {
                        System.out.println("Task was canceled.");
                        break;
                    }
                    System.out.println("Updated file: " + file.getName() + " " + file.getPath());
                    Turbo.getInstance().update(file);
                }

                Turbo.buildContent(project, toolWindow, parentDisposer);
            }
        });
    }
}
