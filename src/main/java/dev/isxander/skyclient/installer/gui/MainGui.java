package dev.isxander.skyclient.installer.gui;

import dev.isxander.skyclient.installer.SkyClient;
import dev.isxander.skyclient.installer.repo.entry.EntryAction;
import dev.isxander.skyclient.installer.repo.entry.EntryWarning;
import dev.isxander.skyclient.installer.repo.entry.ModEntry;
import dev.isxander.skyclient.installer.repo.entry.PackEntry;
import dev.isxander.skyclient.installer.utils.*;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class MainGui {

    private final SkyClient sc;

    private final Map<ModEntry, GuiEntry> modEntries;
    private final Map<PackEntry, GuiEntry> packEntries;

    public MainGui(SkyClient sc) {
        this.sc = sc;
        this.modEntries = new HashMap<>();
        this.packEntries = new HashMap<>();

        sc.getRepositoryManager().fetchFiles();

        Image icon = FileUtils.getResourceImage("/skyclient.png");

        JFrame frame = new JFrame("SkyClient Installer (Xander Edition)");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setIconImage(icon);
        frame.setResizable(false);

        Container container = frame.getContentPane();

        JPanel modPane = new JPanel();
        JPanel packPane = new JPanel();

        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();

        container.setLayout(gridBag);
        modPane.setLayout(gridBag);
        packPane.setLayout(gridBag);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        int i = 0;
        for (ModEntry mod : sc.getRepositoryManager().getModEntries()) {
            boolean visible = !mod.isHidden();

            String description = StringUtils.wrapText(mod.getDescription() + "\n\n- by " + mod.getCreator(), 50);
            JLabel imgLabel = new JLabel(new ImageIcon(ImageUtils.getScaledImage(sc.getRepositoryManager().getImage(mod.getIconFile()), 50, 50, mod.getIconScaling())));
            imgLabel.setName(mod.getId());
            imgLabel.setPreferredSize(new Dimension(50, 50));
            imgLabel.setToolTipText(description);
            constraints.insets = new Insets(1, 1, 1, 1);
            constraints.gridx = 0;
            constraints.gridy = i;
            gridBag.setConstraints(imgLabel, constraints);
            if (visible)
                modPane.add(imgLabel);

            GuiCheckBox checkBox = new GuiCheckBox(mod.getDisplayName()) {
                @Override
                public void onPress() {
                    if (!isSelected() || (warnDanger(mod.getWarning()) && warnExtraPacks(mod.getPackRequirements()))) {
                        mod.setEnabled(isSelected());

                        if (mod.isEnabled()) {
                            for (String modId : mod.getModRequirements()) {
                                ModEntry requiredMod = sc.getRepositoryManager().getMod(modId);
                                requiredMod.setEnabled(true);
                                modEntries.get(requiredMod).checkbox.setEnabled(true);
                            }
                            for (String packId : mod.getPackRequirements()) {
                                PackEntry requiredPack = sc.getRepositoryManager().getPack(packId);
                                requiredPack.setEnabled(true);
                                packEntries.get(requiredPack).checkbox.setEnabled(true);
                            }
                        }

                    }
                    setSelected(mod.isEnabled());
                }
            };
            checkBox.setName(mod.getId());
            checkBox.setSelected(mod.isEnabled());
            checkBox.setToolTipText(description);
            checkBox.addActionListener((action) -> {
                checkBox.onPress();
            });
            constraints.gridx = 1;
            constraints.gridy = i;
            gridBag.setConstraints(checkBox, constraints);
            if (visible)
                modPane.add(checkBox);

            JButton actionButton = new JButton("^");
            actionButton.setPreferredSize(new Dimension(30, 25));
            actionButton.setHorizontalAlignment(SwingConstants.CENTER);
            actionButton.setName(mod.getId());
            if (mod.getActions().length > 0) {
                actionButton.setToolTipText("Mod Actions");
                actionButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        genPopup(mod.getActions()).show(e.getComponent(), e.getX(), e.getY());
                    }
                });
            } else {
                actionButton.setText("");
                actionButton.setEnabled(false);
            }

            constraints.gridx = 3;
            constraints.gridy = i;
            gridBag.setConstraints(actionButton, constraints);
            if (visible)
                modPane.add(actionButton);

            modEntries.put(mod, new GuiEntry(imgLabel, checkBox, actionButton));

            if (visible)
                i++;
        }

        i = 0;
        for (PackEntry pack : sc.getRepositoryManager().getPackEntries()) {
            if (pack.isHidden()) {
                continue;
            }
            String description = StringUtils.wrapText(pack.getDescription() + "\n\n- by " + pack.getCreator(), 50);
            JLabel imgLabel = new JLabel(new ImageIcon(ImageUtils.getScaledImage(sc.getRepositoryManager().getImage(pack.getIconFile()), 50, 50, pack.getIconScaling())));
            imgLabel.setName(pack.getId());
            imgLabel.setToolTipText(description);
            imgLabel.setPreferredSize(new Dimension(50, 50));
            constraints.insets = new Insets(1, 1, 1, 1);
            constraints.gridx = 0;
            constraints.gridy = i;
            gridBag.setConstraints(imgLabel, constraints);
            packPane.add(imgLabel);

            GuiCheckBox checkBox = new GuiCheckBox(pack.getDisplayName()) {
                @Override
                public void onPress() {
                    if (!isSelected() || warnDanger(pack.getWarning())) {
                        pack.setEnabled(isSelected());
                    }
                    setSelected(pack.isEnabled());
                }
            };
            checkBox.setName(pack.getId());
            checkBox.setSelected(pack.isEnabled());
            checkBox.setToolTipText(description);
            checkBox.addActionListener((action) -> {
                checkBox.onPress();
            });
            constraints.gridx = 1;
            constraints.gridy = i;
            gridBag.setConstraints(checkBox, constraints);
            packPane.add(checkBox);

            JButton actionButton = new JButton("^");
            actionButton.setPreferredSize(new Dimension(30, 25));
            actionButton.setHorizontalAlignment(SwingConstants.CENTER);
            actionButton.setName(pack.getId());
            if (pack.getActions().length > 0) {
                actionButton.setToolTipText("Pack Actions");
                actionButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        genPopup(pack.getActions()).show(e.getComponent(), e.getX(), e.getY());
                    }
                });
            } else {
                actionButton.setText("");
                actionButton.setEnabled(false);
            }

            constraints.gridx = 2;
            constraints.gridy = i;
            gridBag.setConstraints(actionButton, constraints);
            packPane.add(actionButton);

            packEntries.put(pack, new GuiEntry(imgLabel, checkBox, actionButton));

            i++;
        }

        sc.getRepositoryManager().getIcons(new UpdateHook() {
            @Override
            public void updateMod(ModEntry mod) {
                refreshModIcon(mod);
            }

            @Override
            public void updatePack(PackEntry pack) {
                refreshPackIcon(pack);
            }
        });

        JButton installButton = new JButton("Install");
        installButton.addActionListener((action) -> {
            try {
                installButton.setEnabled(false);
                installButton.setText("Installing SkyClient... This may take a while.");
                sc.install();
                installButton.setEnabled(true);
                installButton.setText("Install");
                JOptionPane.showMessageDialog(null, "SkyClient has been successfully installed.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        installButton.setPreferredSize(new Dimension(200, 30));
        constraints.insets = new Insets(1, 1, 1, 3);
        constraints.gridwidth = 4;
        constraints.gridx = 0;
        constraints.gridy = 2;
        gridBag.setConstraints(installButton, constraints);
        container.add(installButton);

        JTextField pathDisplayText = new JTextField(sc.getMcDir().getAbsolutePath(), 1);
        pathDisplayText.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onType();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onType();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onType();
            }

            private void onType() {
                sc.setMcDir(new File(pathDisplayText.getText()));
            }
        });
        pathDisplayText.setPreferredSize(new Dimension(150, 30));
        constraints.insets = new Insets(1, 1, 1, 3);
        constraints.gridwidth = 3;
        constraints.gridx = 0;
        constraints.gridy = 3;
        gridBag.setConstraints(pathDisplayText, constraints);
        container.add(pathDisplayText);

        JButton pathButton = new JButton("Select Path");
        pathButton.addActionListener((action) -> {
            JFrame pathFrame = new JFrame("Select Path");
            pathFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            pathFrame.setIconImage(FileUtils.getResourceImage("/skyclient.png"));
            pathFrame.setResizable(false);

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.addActionListener((listener) -> {
                if (listener.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                    sc.setMcDir(fileChooser.getSelectedFile());
                    pathDisplayText.setText(sc.getMcDir().getAbsolutePath());

                    pathFrame.dispose();
                } else if (listener.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
                    pathFrame.dispose();
                }
            });
            fileChooser.setCurrentDirectory(sc.getMcDir());
            fileChooser.setDialogTitle("Select Minecraft Data Folder");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);

            pathFrame.add(fileChooser);
            pathFrame.pack();
            pathFrame.setVisible(true);
        });
        pathButton.setPreferredSize(new Dimension(50, 30));
        constraints.insets = new Insets(1, 1, 1, 3);
        constraints.gridwidth = 1;
        constraints.gridx = 3;
        constraints.gridy = 3;
        gridBag.setConstraints(pathButton, constraints);
        container.add(pathButton);

        JLabel modLabel = new JLabel("Mods", SwingConstants.CENTER);
        modLabel.setPreferredSize(new Dimension(200, 30));
        constraints.gridwidth = 2;
        constraints.gridx = 0;
        constraints.gridy = 0;
        gridBag.setConstraints(modLabel, constraints);
        container.add(modLabel);

        JScrollPane modScrollPane = new JScrollPane(modPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        modScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        modScrollPane.setPreferredSize(new Dimension(370, 500));
        constraints.gridwidth = 2;
        constraints.gridx = 0;
        constraints.gridy = 1;
        gridBag.setConstraints(modScrollPane, constraints);
        container.add(modScrollPane);

        JLabel packLabel = new JLabel("Packs", SwingConstants.CENTER);
        packLabel.setPreferredSize(new Dimension(200, 30));
        constraints.gridwidth = 2;
        constraints.gridx = 2;
        constraints.gridy = 0;
        gridBag.setConstraints(packLabel, constraints);
        container.add(packLabel);

        JScrollPane packScrollPane = new JScrollPane(packPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        packScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        packScrollPane.setPreferredSize(new Dimension(370, 500));
        constraints.gridwidth = 2;
        constraints.gridx = 2;
        constraints.gridy = 1;
        gridBag.setConstraints(packScrollPane, constraints);
        container.add(packScrollPane);

        frame.pack();
        frame.setVisible(true);
    }

    public static boolean warnDanger(EntryWarning warning) {
        if (warning == null)
            return true;

        int option = JOptionPane.showConfirmDialog(null, warning.getMessageHtml().replaceAll("\"", ""), "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return option == JOptionPane.YES_OPTION;
    }

    public static boolean warnExtraMods(String[] modIds) {
        if (modIds.length < 1) return true;

        List<String> warnLines = new ArrayList<>();
        warnLines.add("This mod requires other dependencies to work.");
        warnLines.add("Do you want to add these mods?");
        warnLines.add("");
        warnLines.addAll(Arrays.asList(modIds));

        return warnDanger(new EntryWarning(warnLines));
    }

    public static boolean warnExtraPacks(String[] packIds) {
        if (packIds.length < 1) return true;

        List<String> warnLines = new ArrayList<>();
        warnLines.add("This mod requires resource packs to work.");
        warnLines.add("Do you want to add these packs?");
        warnLines.add("");
        warnLines.addAll(Arrays.asList(packIds));

        return warnDanger(new EntryWarning(warnLines));
    }

    public static void openGuide(String rawText) {
        JFrame guide = new JFrame("Guide");
        guide.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        guide.setIconImage(FileUtils.getResourceImage("/skyclient.png"));
        guide.setResizable(false);

        JPanel pane = new JPanel();
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        guide.setLayout(gridBag);
        pane.setLayout(gridBag);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        Parser parser = Parser.builder().build();
        Node document = parser.parse(rawText);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String html = renderer.render(document);
        System.out.println(html);

        JEditorPane editorPane = new JEditorPane();
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet style = new StyleSheet();
        style.importStyleSheet(MainGui.class.getResource("/md-style.css"));
        kit.setStyleSheet(style);
        editorPane.setEditorKit(kit);
//        editorPane.setContentType("text/html");
//        editorPane.getEditorKit().
        editorPane.setEditable(false);
        editorPane.setText(html);
        pane.add(editorPane);

        JScrollPane sp = new JScrollPane(pane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setPreferredSize(new Dimension(800, 600));
        constraints.gridx = constraints.gridy = constraints.gridwidth = 0;
        gridBag.setConstraints(sp, constraints);
        guide.add(sp);

        guide.pack();
        guide.setVisible(true);
    }

//    private List<JLabel> mdToLabels(String md) {
//        List<JLabel> labels = new ArrayList<>();
//        String textParsed =
//    }

    public static JPopupMenu genPopup(EntryAction[] actions){
        JPopupMenu popup = new JPopupMenu();

        for (EntryAction action : actions) {
            JMenuItem menuItem = new JMenuItem(action.getDisplay());
            menuItem.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    action.getAction().run();
                }
            });
            popup.add(menuItem);
        }

        popup.setSize(popup.getWidth() * 4, popup.getHeight() * 4);
        return popup;
    }

    public void refreshModIcon(ModEntry mod) {
        modEntries.get(mod).imageLabel.setIcon(new ImageIcon(ImageUtils.getScaledImage(sc.getRepositoryManager().getImage(mod.getIconFile()), 50, 50, mod.getIconScaling())));
    }

    public void refreshPackIcon(PackEntry pack) {
        Log.info("Refreshing: " + pack.getId());
        packEntries.get(pack).imageLabel.setIcon(new ImageIcon(ImageUtils.getScaledImage(sc.getRepositoryManager().getImage(pack.getIconFile()), 50, 50, pack.getIconScaling())));
    }

    private static abstract class GuiCheckBox extends JCheckBox {
        public GuiCheckBox(String text) {
            super(text);
        }

        public abstract void onPress();
    }

    private static class GuiEntry {
        public final JLabel imageLabel;
        public final GuiCheckBox checkbox;
        public final JButton actionButton;

        public GuiEntry(JLabel imageLabel, GuiCheckBox checkbox, JButton actionButton) {
            this.imageLabel = imageLabel;
            this.checkbox = checkbox;
            this.actionButton = actionButton;
        }
    }

}
