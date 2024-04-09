package com.turbo.js.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Turbo {
    private static final Turbo INSTANCE = new Turbo();
    private static Content content;
    private final CopyOnWriteArrayList<VirtualFile> turboFiles = new CopyOnWriteArrayList<>();

    private Turbo() { // Private
    }

    public static Turbo getInstance() {
        return INSTANCE;
    }

    public static void buildContent(Project project, ToolWindow toolWindow, Disposable parent) {
        SwingUtilities.invokeLater(() -> {
            if (content != null) {
                Disposer.dispose(content);
            }

            String basePath = Objects.requireNonNull(project.getBasePath());
            TurboToolWindowContent toolWindowContent = getTurboToolWindowContent(toolWindow, basePath, basePath);
            content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), null, false);

            ContentManager manager = toolWindow.getContentManager();
            manager.addContent(content);

            Disposer.register(manager, parent);
            Disposer.register(content, parent);
        });
    }

    private static @NotNull TurboToolWindowContent getTurboToolWindowContent(ToolWindow toolWindow, String projectName, String basePath) {
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(projectName.substring(projectName.lastIndexOf("/") + 1));
        final Map<String, DefaultMutableTreeNode> parentNodes = new HashMap<>();

        for (VirtualFile turboFile : INSTANCE.turboFiles) {
            final String relativePath = turboFile.getPath().replace(basePath, "").replace("\\", "/");
            final String nonTurboPath = relativePath.replace("/turbo.json", "");

            if (nonTurboPath.isEmpty()) {
                continue;
            }

            final String grandparentPath = turboFile.getParent().getParent().getPath();
            final String cleaGrandparentPath = grandparentPath.substring(grandparentPath.lastIndexOf("/") + 1);
            DefaultMutableTreeNode parentDir = parentNodes.computeIfAbsent(cleaGrandparentPath, DefaultMutableTreeNode::new);
            parentDir.add(new DefaultMutableTreeNode(nonTurboPath.replace(cleaGrandparentPath, "").replace("/", "")));
        }

        for (DefaultMutableTreeNode parentNode : parentNodes.values()) {
            root.add(parentNode);
        }

        return new TurboToolWindowContent(toolWindow, root);
    }


    public CopyOnWriteArrayList<VirtualFile> getTurboFiles() {
        return turboFiles;
    }

    public void add(VirtualFile file) {
        turboFiles.add(file);
    }

    public void update(VirtualFile file) {
        int index = turboFiles.indexOf(file);
        if (index != -1) turboFiles.set(index, file);
    }

    private static class TurboToolWindowContent {
        private static final String TURBO_JSON_PNG_PATH = "/icons/turbo-json-13.png";

        private final JPanel contentPanel = new JPanel();
        private final ToolWindow toolWindow;

        public TurboToolWindowContent(ToolWindow toolWindow, DefaultMutableTreeNode projectFilesNode) {
            this.toolWindow = toolWindow;
            setupContentPanel(projectFilesNode);
        }

        private void setupContentPanel(DefaultMutableTreeNode projectFilesNode) {
            contentPanel.setLayout(new BorderLayout(0, 0));
            JTree tree = getjTree(projectFilesNode);

            JScrollPane treeView = new JBScrollPane(tree);
            treeView.setBorder(null); // Ensure scroll pane has no unwanted border
            contentPanel.add(treeView, BorderLayout.CENTER);
        }


        private @NotNull JTree getjTree(DefaultMutableTreeNode root) {
            JTree tree = new Tree(root);
            tree.setCellRenderer(new DefaultTreeCellRenderer() {
                @Override
                public Color getBackgroundSelectionColor() {
                    return new Color(0, 0, 0, 0);
                }

                @Override
                public Color getBackgroundNonSelectionColor() {
                    return UIManager.getColor("Tree.background");
                }

                @Override
                public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                    super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                    setBorderSelectionColor(null);

                    if (value.toString().contains("supaweb") || value.toString().contains("config-eslint") || value.toString().contains("config-prettier") || value.toString().contains("config-tailwind") || value.toString().contains("ui") || value.toString().contains("(root)")) {
                        URL turboJsonIconURL = getClass().getResource(TURBO_JSON_PNG_PATH);
                        if (turboJsonIconURL != null) {
                            setIcon(new ImageIcon(turboJsonIconURL));
                        }
                    } else if (value.toString().contains("Gear Setting")) {
                        // https://icons8.com/icon/85165/play
                        URL folderIconURL = getClass().getResource("/icons/settings-icon.png");
                        if (folderIconURL != null) {
                            setIcon(new ImageIcon(folderIconURL));
                        }
                    } else {
                        URL folderIconURL;
                        if (expanded) {
                            // https://icons8.com/icon/eAX0829vLotw/folder
                            folderIconURL = getClass().getResource("/icons/folder-icon-blue-open.png");
                        } else {
                            // https://icons8.com/icon/R2UiKrOmA89S/opened-folder
                            folderIconURL = getClass().getResource("/icons/folder-icon-blue.png");
                        }
                        setIcon(new ImageIcon(Objects.requireNonNull(folderIconURL)));
                    }

                    return this;
                }
            });
            return tree;
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }
    }
}