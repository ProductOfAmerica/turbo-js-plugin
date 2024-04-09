package com.turbo.js.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.turbo.js.FileEventListener;
import com.turbo.js.FilesLoadedListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class FilesLoadedListenerImpl extends FileEventListener implements FilesLoadedListener {
    public FilesLoadedListenerImpl(Project project, ToolWindow toolWindow, Disposable parentDisposer) {
        super(project, toolWindow, parentDisposer);
    }

    @Override
    public void onFilesLoaded(ArrayList<VirtualFile> files) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading turbo files", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                System.out.println("Turbo files loaded!");

                for (VirtualFile file : files) {
                    if (indicator.isCanceled()) {
                        System.out.println("Task was canceled.");
                        break;
                    }
                    System.out.println("Loaded file: " + file.getName() + " " + file.getPath());
                    Turbo.getInstance().add(file);
                }

                Turbo.buildContent(project, toolWindow, parentDisposer);
            }
        });
    }
}
