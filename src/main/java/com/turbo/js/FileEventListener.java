package com.turbo.js;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;

public abstract class FileEventListener {
    protected final Project project;
    protected final ToolWindow toolWindow;
    protected final Disposable parentDisposer;

    public FileEventListener(Project project, ToolWindow toolWindow, Disposable parentDisposer) {
        this.project = project;
        this.toolWindow = toolWindow;
        this.parentDisposer = parentDisposer;
    }
}
