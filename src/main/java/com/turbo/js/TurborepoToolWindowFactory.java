package com.turbo.js;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

final class TurboJSToolWindowFactory implements ToolWindowFactory, DumbAware {
    private static final Logger LOG = Logger.getInstance(TurboJSToolWindowFactory.class);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        File projectDirectory = new File(Objects.requireNonNull(project.getBasePath()));
        DefaultMutableTreeNode projectFilesNode = loadProjectFiles(projectDirectory);
        TurboToolWindowContent toolWindowContent = new TurboToolWindowContent(toolWindow, projectFilesNode);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private DefaultMutableTreeNode loadProjectFiles(File directory) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(directory.getName());

        File[] directoryFiles = directory.listFiles();
        if (Objects.isNull(directoryFiles)) {
            return root;
        }

        final ArrayList<String> workspaceDirs = new ArrayList<>();
        boolean isMonorepo = false;

        for (File file : directoryFiles) {
            if (file.getName().equals("pnpm-workspace.yaml") || file.getName().equals("package.json")) {
                isMonorepo = true;
                parseWorkspaceConfig(file, workspaceDirs);
                break;
            }
        }

        if (isMonorepo && !workspaceDirs.isEmpty()) {
            for (String dir : workspaceDirs) {
                File workspaceDir = new File(directory, dir);
                if (workspaceDir.exists() && workspaceDir.isDirectory()) {
                    DefaultMutableTreeNode subPackageNode = new DefaultMutableTreeNode(workspaceDir.getName());
                    for (File subPackage : Objects.requireNonNull(workspaceDir.listFiles())) {
                        if (subPackage.isDirectory() && new File(subPackage, "turbo.json").exists()) {
                            subPackageNode.add(new DefaultMutableTreeNode(subPackage.getName()));
                            root.add(subPackageNode);
                        }
                    }
                }
            }
        }

        if (Arrays.stream(directoryFiles).anyMatch(file -> "turbo.json".equals(file.getName())))
            root.add(new DefaultMutableTreeNode("(root)"));

        return root;
    }

    public void parseWorkspaceConfig(File configFile, ArrayList<String> workspaceDirs) {
        String fileName = configFile.getName();
        if (fileName.equals("package.json")) {
            parseNpmOrYarnWorkspaceConfig(configFile, workspaceDirs);
        } else if (fileName.equals("pnpm-workspace.yaml")) {
            parsePnpmWorkspaceConfig(configFile, workspaceDirs);
        }
    }

    private void parseNpmOrYarnWorkspaceConfig(File configFile, ArrayList<String> workspaceDirs) {
        Path filePath = configFile.toPath();
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            String fileContent = String.join("\n", lines);
            JSONObject json = new JSONObject(fileContent);
            if (json.has("workspaces")) {
                Object workspaces = json.get("workspaces");
                if (workspaces instanceof JSONObject) {
                    workspaces = ((JSONObject) workspaces).getJSONArray("packages");
                }
                for (Object workspace : (JSONArray) workspaces) {
                    String dir = workspace.toString().replace("/*", "");
                    workspaceDirs.add(dir);
                }
            }
        } catch (JSONException | IOException e) {
            LOG.error("An error occurred while parsing the Yarn workspace configuration.", e);
        }
    }

    private void parsePnpmWorkspaceConfig(File configFile, ArrayList<String> workspaceDirs) {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            Yaml yaml = new Yaml();
            Iterable<Object> it = yaml.loadAll(reader);
            for (Object o : it) {
                if (o instanceof Map<?, ?> map) {
                    Object packagesObj = map.get("packages");
                    if (packagesObj instanceof List<?> packagesList) {
                        for (Object pkgObj : packagesList) {
                            if (pkgObj instanceof String pkg) {
                                String dir = pkg.replace("/*", "");
                                workspaceDirs.add(dir);
                            } else {
                                LOG.error("Non-string value found in 'packages' list.");
                            }
                        }
                    } else if (packagesObj != null) {
                        LOG.error("Expected 'packages' to be a list, but found: " + packagesObj.getClass());
                    }
                } else {
                    LOG.error("Expected a map but found: " + o.getClass());
                }
            }
        } catch (Exception e) {
            LOG.error("An error occurred while parsing the PNPM workspace configuration.", e);
        }
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
