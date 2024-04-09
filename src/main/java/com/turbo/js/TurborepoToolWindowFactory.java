package com.turbo.js;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.turbo.js.impl.FilesLoadedListenerImpl;
import com.turbo.js.impl.FilesUpdatedListenerImpl;
import com.turbo.js.impl.TurboJsonFileListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

final class TurboJSToolWindowFactory implements ToolWindowFactory, DumbAware, Disposable {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        initializeEventBus(project, toolWindow);
        initializeFileSystem(project);
    }

    @Override
    public void dispose() {
        System.out.println(TurboJSToolWindowFactory.class.getName() + " is disposing...");
    }

    private void initializeEventBus(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        final MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(FilesLoadedListenerImpl.LOADED_TOPIC, new FilesLoadedListenerImpl(project, toolWindow, this));
        connection.subscribe(FilesUpdatedListenerImpl.UPDATE_TOPIC, new FilesUpdatedListenerImpl(project, toolWindow, this));
        Disposer.register(connection, this);
    }

    private void initializeFileSystem(@NotNull Project project) {
        VirtualFileManager.getInstance().addAsyncFileListener(new TurboJsonFileListener(project), this);

        AppExecutorUtil.getAppExecutorService().execute(() -> ApplicationManager.getApplication().runReadAction(() -> {
            ArrayList<VirtualFile> files = new ArrayList<>();
            ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);

            Arrays.stream(ProjectRootManager.getInstance(project).getContentRoots()).filter(root -> !fileIndex.isExcluded(root)).forEach(root -> VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor<Void>() {
                @Override
                public boolean visitFile(@NotNull VirtualFile file) {
                    if (!fileIndex.isExcluded(file) && file.getName().equals("turbo.json")) {
                        files.add(file);
                    }
                    return true; // Continue visiting
                }
            }));

            project.getMessageBus().syncPublisher(FilesLoadedListenerImpl.LOADED_TOPIC).onFilesLoaded(files);
        }));
    }
}