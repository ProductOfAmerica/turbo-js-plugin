package com.turbo.js;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.turbo.js.impl.TurboJsonFileListener;
import com.turbo.js.listener.FilesLoadedListener;
import com.turbo.js.listener.FilesUpdatedListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import java.util.ArrayList;
import java.util.Arrays;

import static com.turbo.js.listener.FilesLoadedListener.LOADED_TOPIC;
import static com.turbo.js.listener.FilesUpdatedListener.UPDATE_TOPIC;

final class TurboJSToolWindowFactory implements ToolWindowFactory, DumbAware, Disposable {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        initializeEventBus(project);
        initializeFileSystem(project);
        initializeContent(toolWindow);
    }

    @Override
    public void dispose() {
        System.out.println(TurboJSToolWindowFactory.class.getName() + " is disposing...");
    }

    private void initializeContent(ToolWindow toolWindow) {
        Content content = ContentFactory.getInstance().createContent(new JPanel(), null, false);
        content.setDisposer(this);

        ContentManager manager = toolWindow.getContentManager();
        manager.addContent(content);
        Disposer.register(manager, this);
    }

    private void initializeEventBus(@NotNull Project project) {
        MessageBusConnection connection = project.getMessageBus().connect();

        connection.subscribe(LOADED_TOPIC, (FilesLoadedListener) files -> {
            System.out.println("Files loaded!");
            for (VirtualFile file : files) {
                System.out.println("Loaded file: " + file.getName() + " " + file.getPath());
            }
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Parsing workspace configuration", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
//                    ArrayList<String> workspaceDirs = new ArrayList<>();
//                    parseWorkspaceConfig(configFile, workspaceDirs);
                    // Assuming you want to do something with workspaceDirs after parsing
                    // This could be updating the UI or performing further operations
                }
            });
        });

        connection.subscribe(UPDATE_TOPIC, (FilesUpdatedListener) files -> {
            System.out.println("Files updated!");
            for (VirtualFile file : files) {
                System.out.println("Updated file: " + file.getName() + " " + file.getPath());
            }
        });

        Disposer.register(connection, this);
    }

    private void initializeFileSystem(@NotNull Project project) {
        VirtualFileManager.getInstance().addAsyncFileListener(new TurboJsonFileListener(project), this);

        AppExecutorUtil.getAppExecutorService().execute(() -> ApplicationManager.getApplication().runReadAction(() -> {
            ArrayList<VirtualFile> files = new ArrayList<>();
            ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);

            Arrays.stream(ProjectRootManager.getInstance(project).getContentRoots())
                    .filter(root -> !fileIndex.isExcluded(root))
                    .forEach(root -> VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor<Void>() {
                        @Override
                        public boolean visitFile(@NotNull VirtualFile file) {
                            if (!fileIndex.isExcluded(file) && file.getName().equals("turbo.json")) {
                                files.add(file);
                            }
                            return true; // Continue visiting
                        }
                    }));

            project.getMessageBus().syncPublisher(LOADED_TOPIC).onFilesLoaded(files);
        }));
    }


//
//    private DefaultMutableTreeNode loadProjectFiles(File directory) {
//        DefaultMutableTreeNode root = new DefaultMutableTreeNode(directory.getName());
//
//        File[] directoryFiles = directory.listFiles();
//        if (directoryFiles == null) {
//            return root;
//        }
//
//        final ArrayList<String> workspaceDirs = new ArrayList<>();
//        boolean isMonorepo = false;
//
//        for (File file : directoryFiles) {
//            if (file.getName().equals("pnpm-workspace.yaml") || file.getName().equals("package.json")) {
//                isMonorepo = true;
//                parseWorkspaceConfig(file, workspaceDirs);
//                break;
//            }
//        }
//
//        if (isMonorepo && !workspaceDirs.isEmpty()) {
//            for (String dir : workspaceDirs) {
//                File workspaceDir = new File(directory, dir);
//                if (workspaceDir.exists() && workspaceDir.isDirectory()) {
//                    DefaultMutableTreeNode subPackageNode = new DefaultMutableTreeNode(workspaceDir.getName());
//                    for (File subPackage : Objects.requireNonNull(workspaceDir.listFiles())) {
//                        if (subPackage.isDirectory() && new File(subPackage, "turbo.json").exists()) {
//                            subPackageNode.add(new DefaultMutableTreeNode(subPackage.getName()));
//                            root.add(subPackageNode);
//                        }
//                    }
//                }
//            }
//        }
//
//        if (Arrays.stream(directoryFiles).anyMatch(file -> "turbo.json".equals(file.getName())))
//            root.add(new DefaultMutableTreeNode("(root)"));
//
//        return root;
//    }
//
//    public void parseWorkspaceConfig(File configFile, ArrayList<String> workspaceDirs) {
//        String fileName = configFile.getName();
//        if (fileName.equals("package.json")) {
//            parseNpmOrYarnWorkspaceConfig(configFile, workspaceDirs);
//        } else if (fileName.equals("pnpm-workspace.yaml")) {
//            parsePnpmWorkspaceConfig(configFile, workspaceDirs);
//        }
//    }
//
//    private void parseNpmOrYarnWorkspaceConfig(File configFile, ArrayList<String> workspaceDirs) {
//        Path filePath = configFile.toPath();
//        try {
//            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
//            String fileContent = String.join("\n", lines);
//            JSONObject json = new JSONObject(fileContent);
//
//            if (json.has("workspaces")) {
//                Object workspaces = json.get("workspaces");
//                if (workspaces instanceof JSONObject) {
//                    workspaces = ((JSONObject) workspaces).getJSONArray("packages");
//                }
//                for (Object workspace : (JSONArray) workspaces) {
//                    String dir = workspace.toString().replace("/*", "");
//                    workspaceDirs.add(dir);
//                }
//            }
//        } catch (JSONException | IOException e) {
//            LOG.error("An error occurred while parsing the Yarn workspace configuration:", e);
//        }
//    }
//
//    private void parsePnpmWorkspaceConfig(File configFile, ArrayList<String> workspaceDirs) {
//        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
//            for (Object o : new Yaml().loadAll(reader)) {
//                if (!(o instanceof Map<?, ?> map)) {
//                    LOG.error("Expected a map but found: " + o.getClass());
//                    continue;
//                }
//
//                Object packagesObj = map.get("packages");
//                if (!(packagesObj instanceof List<?> packagesList)) {
//                    if (packagesObj != null) {
//                        LOG.error("Expected 'packages' to be a list, but found: " + packagesObj.getClass());
//                    }
//                    return;
//                }
//
//                for (Object pkgObj : packagesList) {
//                    if (pkgObj instanceof String pkg) {
//                        String dir = pkg.replace("/*", "");
//                        workspaceDirs.add(dir);
//                    } else {
//                        LOG.error("Non-string value found in 'packages' list. (" + packagesObj.getClass() + ")");
//                    }
//                }
//            }
//        } catch (Exception e) {
//            LOG.error("An error occurred while parsing the PNPM workspace configuration:", e);
//        }
//    }
//
//    private static class TurboToolWindowContent {
//        private static final String TURBO_JSON_PNG_PATH = "/icons/turbo-json-13.png";
//
//        private final JPanel contentPanel = new JPanel();
//        private final ToolWindow toolWindow;
//
//        public TurboToolWindowContent(ToolWindow toolWindow, DefaultMutableTreeNode projectFilesNode) {
//            this.toolWindow = toolWindow;
//            setupContentPanel(projectFilesNode);
//        }
//
//        private void setupContentPanel(DefaultMutableTreeNode projectFilesNode) {
//            contentPanel.setLayout(new BorderLayout(0, 0));
//            JTree tree = getjTree(projectFilesNode);
//
//            JScrollPane treeView = new JBScrollPane(tree);
//            treeView.setBorder(null); // Ensure scroll pane has no unwanted border
//            contentPanel.add(treeView, BorderLayout.CENTER);
//        }
//
//
//        private @NotNull JTree getjTree(DefaultMutableTreeNode root) {
//            JTree tree = new Tree(root);
//            tree.setCellRenderer(new DefaultTreeCellRenderer() {
//                @Override
//                public Color getBackgroundSelectionColor() {
//                    return new Color(0, 0, 0, 0);
//                }
//
//                @Override
//                public Color getBackgroundNonSelectionColor() {
//                    return UIManager.getColor("Tree.background");
//                }
//
//                @Override
//                public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
//                    super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
//
//                    setBorderSelectionColor(null);
//
//                    if (value.toString().contains("supaweb") || value.toString().contains("config-eslint") || value.toString().contains("config-prettier") || value.toString().contains("config-tailwind") || value.toString().contains("ui") || value.toString().contains("(root)")) {
//                        URL turboJsonIconURL = getClass().getResource(TURBO_JSON_PNG_PATH);
//                        if (turboJsonIconURL != null) {
//                            setIcon(new ImageIcon(turboJsonIconURL));
//                        }
//                    } else if (value.toString().contains("Gear Setting")) {
//                        // https://icons8.com/icon/85165/play
//                        URL folderIconURL = getClass().getResource("/icons/settings-icon.png");
//                        if (folderIconURL != null) {
//                            setIcon(new ImageIcon(folderIconURL));
//                        }
//                    } else {
//                        URL folderIconURL;
//                        if (expanded) {
//                            // https://icons8.com/icon/eAX0829vLotw/folder
//                            folderIconURL = getClass().getResource("/icons/folder-icon-blue-open.png");
//                        } else {
//                            // https://icons8.com/icon/R2UiKrOmA89S/opened-folder
//                            folderIconURL = getClass().getResource("/icons/folder-icon-blue.png");
//                        }
//                        setIcon(new ImageIcon(Objects.requireNonNull(folderIconURL)));
//                    }
//
//                    return this;
//                }
//            });
//            return tree;
//        }
//
//        public JPanel getContentPanel() {
//            return contentPanel;
//        }
//    }
}
