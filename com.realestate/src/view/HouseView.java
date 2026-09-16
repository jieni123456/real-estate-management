package view;

import controller.HouseController;
import util.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.function.Consumer;

public class HouseView extends JPanel {

    private static final String[] COLUMNS =
            {"ID", "户型", "面积(m²)", "地址", "房东ID", "房东姓名", "房东电话"};

    private final HouseController houseController;
    private final Consumer<String> statusReporter;

    private final JTable houseTable;
    private final JButton deleteButton = new JButton("删除房屋");

    public HouseView(HouseController houseController, Consumer<String> statusReporter) {
        this.houseController = houseController;
        this.statusReporter = statusReporter;

        setLayout(new BorderLayout());
        setBackground(Theme.PAGE_BG);
        setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = createHeader();
        houseTable = createHouseTable();

        JScrollPane scrollPane = new JScrollPane(houseTable);
        scrollPane.setBorder(new LineBorder(Theme.BORDER, 1, true));
        scrollPane.getViewport().setBackground(Theme.SURFACE);

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        applyDeletePermission();
        refresh();
    }

    // ------------------------------------------------------------ 顶部区域

    private JPanel createHeader() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 14, 0));

        JLabel title = new JLabel("房屋信息管理");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_HEADING);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);

        JButton addButton = primaryButton("添加房屋");
        addButton.addActionListener(e -> showAddHouseDialog());

        deleteButton.setFont(Theme.FONT_BODY);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> deleteSelectedHouse());

        JButton refreshButton = secondaryOutlineButton("刷新数据");
        refreshButton.addActionListener(e -> refresh());

        buttons.add(addButton);
        buttons.add(deleteButton);
        buttons.add(refreshButton);

        top.add(title, BorderLayout.NORTH);
        top.add(buttons, BorderLayout.SOUTH);
        return top;
    }

    private JTable createHouseTable() {
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setFont(Theme.FONT_BODY);
        table.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Theme.BORDER_LIGHT);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setBackground(Theme.SURFACE);
        table.setSelectionBackground(Theme.ACCENT);
        table.setSelectionForeground(Theme.TEXT_ON_ACCENT);

        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.FONT_TABLE_HEADER);
        header.setBackground(Theme.TABLE_HEADER_BG);
        header.setForeground(new Color(0x3C, 0x40, 0x43));
        header.setReorderingAllowed(false);
        Dimension headerSize = header.getPreferredSize();
        header.setPreferredSize(new Dimension(headerSize.width, 38));
        return table;
    }

    // ------------------------------------------------------------ 数据加载

    /** 重新读取并刷新表格，同时把记录数写入底部状态栏 */
    public void refresh() {
        DefaultTableModel model = (DefaultTableModel) houseTable.getModel();
        model.setRowCount(0);

        List<Object[]> houses = houseController.getAllHouses();
        for (Object[] house : houses) {
            model.addRow(house);
        }

        if (statusReporter != null) {
            statusReporter.accept("共 " + houses.size() + " 条房屋记录");
        }
    }

    // ------------------------------------------------------------ 权限控制

    /**
     * 按当前用户权限启用或置灰「删除房屋」按钮。
     *
     * <p>界面层的置灰只是体验优化——真正的防护在 {@code HouseController.deleteHouse}。
     *
     * <p>这里显式指定禁用态的配色，而不依赖外观库的默认处理，
     * 以确保「灰底灰字」与正常的「红字红边」对比足够鲜明，用户一眼能看出是权限不足。
     */
    private void applyDeletePermission() {
        boolean allowed = houseController.canDelete();
        deleteButton.setEnabled(allowed);

        if (allowed) {
            deleteButton.setForeground(Theme.DANGER);
            deleteButton.setBackground(Theme.SURFACE);
            deleteButton.setBorder(outlineBorder(Theme.DANGER));
            deleteButton.setToolTipText(null);
        } else {
            deleteButton.setForeground(Theme.DISABLED_FG);
            deleteButton.setBackground(Theme.DISABLED_BG);
            deleteButton.setBorder(outlineBorder(Theme.DISABLED_BORDER));
            deleteButton.setToolTipText("需要管理员权限");
        }
        deleteButton.repaint();
    }

    // ------------------------------------------------------------ 删除逻辑

    private void deleteSelectedHouse() {
        int selectedRow = houseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先选择要删除的房屋", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String houseId = String.valueOf(houseTable.getValueAt(selectedRow, 0));
        String address = String.valueOf(houseTable.getValueAt(selectedRow, 3));

        Object[] options = {"取消", "确认删除"};
        int choice = JOptionPane.showOptionDialog(this,
                "确定要删除房屋 " + houseId + "（" + address + "）吗？此操作不可撤销。",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]); // 默认焦点在「取消」，避免习惯性回车误删

        if (choice != 1) {
            return;
        }

        if (houseController.deleteHouse(houseId)) {
            Toast.success(this, "房屋删除成功");
            refresh();
        } else {
            JOptionPane.showMessageDialog(this, "删除房屋失败", "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------- 添加对话框

    private void showAddHouseDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "添加新房屋",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());

        JLabel title = new JLabel("添加新房屋");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_HEADING);
        title.setBorder(new EmptyBorder(18, 20, 0, 20));
        dialog.add(title, BorderLayout.NORTH);

        JTextField houseId = new JTextField();
        JTextField type = new JTextField();
        JTextField area = new JTextField();
        JTextField address = new JTextField();
        JTextField landlordId = new JTextField();
        JTextField landlordName = new JTextField();
        JTextField landlordContact = new JTextField();

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 20, 0, 20));

        body.add(groupLabel("房屋信息"));
        body.add(twoColumnForm(
                new String[]{"房屋ID", "户型", "面积(m²)", "地址"},
                new JTextField[]{houseId, type, area, address}));
        body.add(Box.createVerticalStrut(18));
        body.add(groupLabel("房东信息"));
        body.add(twoColumnForm(
                new String[]{"房东ID", "姓名", "电话"},
                new JTextField[]{landlordId, landlordName, landlordContact}));

        dialog.add(body, BorderLayout.CENTER);

        JButton cancel = secondaryOutlineButton("取消");
        cancel.addActionListener(e -> dialog.dispose());

        JButton submit = primaryButton("提交");
        submit.addActionListener(e -> {
            if (houseId.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "房屋ID不能为空", "输入错误",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            double areaValue;
            try {
                areaValue = Double.parseDouble(area.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "面积必须是数字", "输入错误",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = houseController.addHouse(
                    houseId.getText().trim(),
                    type.getText().trim(),
                    areaValue,
                    address.getText().trim(),
                    landlordId.getText().trim(),
                    landlordName.getText().trim(),
                    landlordContact.getText().trim());

            if (success) {
                Toast.success(this, "房屋添加成功");
                refresh();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "房屋添加失败", "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(20, 20, 18, 20));
        actions.add(cancel);
        actions.add(submit);
        dialog.add(actions, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ---------------------------------------------------------------- 小工具

    private JLabel groupLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_SUBTITLE);
        label.setForeground(Theme.ACCENT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(0, 0, 6, 0)));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return label;
    }

    /** 两列排布的表单：每行两组「标签 + 输入框」 */
    private JPanel twoColumnForm(String[] labels, JTextField[] fields) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < fields.length; i++) {
            int column = (i % 2) * 2;
            int row = i / 2;

            JLabel label = new JLabel(labels[i]);
            label.setFont(Theme.FONT_CAPTION);
            label.setForeground(Theme.TEXT_SECONDARY);

            JTextField field = fields[i];
            field.setFont(Theme.FONT_BODY);
            field.setPreferredSize(new Dimension(190, 30));

            gbc.gridx = column;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.insets = new Insets(0, 0, 10, 10);
            panel.add(label, gbc);

            gbc.gridx = column + 1;
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 0, 10, 18);
            panel.add(field, gbc);
        }
        return panel;
    }

    private CompoundBorder outlineBorder(Color color) {
        return new CompoundBorder(new LineBorder(color, 1, true),
                new EmptyBorder(5, 14, 5, 14));
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(Theme.FONT_BODY);
        button.setBackground(Theme.ACCENT);
        button.setForeground(Theme.TEXT_ON_ACCENT);
        button.setBorder(new EmptyBorder(6, 16, 6, 16));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton secondaryOutlineButton(String text) {
        JButton button = new JButton(text);
        button.setFont(Theme.FONT_BODY);
        button.setBackground(Theme.SURFACE);
        button.setForeground(Theme.TEXT_PRIMARY);
        button.setBorder(outlineBorder(Theme.BORDER_INPUT));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
