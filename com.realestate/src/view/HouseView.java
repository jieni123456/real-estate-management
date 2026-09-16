package view;

import controller.HouseController;
import model.House;
import util.Result;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HouseView extends JPanel {

    private static final String[] COLUMNS =
            {"ID", "户型", "面积(m²)", "地址", "房东ID", "房东姓名", "房东电话"};

    private final HouseController houseController;
    private final Consumer<String> statusReporter;

    private final JTable houseTable;
    private final JButton editButton = new JButton("编辑房屋");
    private final JButton deleteButton = new JButton("删除房屋");

    /** 与表格行一一对应的数据，避免再从表格单元格里反解字段 */
    private List<House> currentHouses = new ArrayList<>();

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

        applyPermissions();
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
        addButton.addActionListener(e -> showHouseDialog(null));

        editButton.setFont(Theme.FONT_BODY);
        editButton.setFocusPainted(false);
        editButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editButton.addActionListener(e -> editSelectedHouse());

        deleteButton.setFont(Theme.FONT_BODY);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> deleteSelectedHouse());

        JButton refreshButton = secondaryOutlineButton("刷新数据");
        refreshButton.addActionListener(e -> refresh());

        // 编辑与刷新同为次要操作，用同一套描边样式
        styleAsSecondary(editButton);

        buttons.add(addButton);
        buttons.add(editButton);
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
        currentHouses = houseController.getAllHouses();

        DefaultTableModel model = (DefaultTableModel) houseTable.getModel();
        model.setRowCount(0);
        for (House house : currentHouses) {
            model.addRow(new Object[]{
                    house.getId(),
                    house.getType(),
                    formatArea(house.getArea()),
                    house.getAddress(),
                    house.getLandlord().getId(),
                    house.getLandlord().getName(),
                    house.getLandlord().getContact()
            });
        }

        if (statusReporter != null) {
            statusReporter.accept("共 " + currentHouses.size() + " 条房屋记录");
        }
    }

    /** 128.0 显示为 128，89.5 保持 89.5 */
    private String formatArea(double area) {
        if (area == Math.rint(area) && !Double.isInfinite(area)) {
            return String.valueOf((long) area);
        }
        return String.valueOf(area);
    }

    // ------------------------------------------------------------ 权限控制

    /**
     * 按当前用户权限启用或置灰「删除房屋」按钮。
     *
     * <p><b>登录成功后必须由 MainView 再次调用本方法。</b>本视图是在登录之前就被
     * 构造的（见 RealEstateSystem.main），那时 Session 里还没有用户，
     * {@code Session.can} 按 fail-safe 返回 false，按钮必然是禁用态。
     *
     * <p>界面层的置灰只是体验优化——真正的防护在 {@code HouseController.deleteHouse}。
     *
     * <p>这里显式指定禁用态的配色，而不依赖外观库的默认处理，
     * 以确保「灰底灰字」与正常的「红字红边」对比足够鲜明，用户一眼能看出是权限不足。
     */
    public void applyPermissions() {
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

    // ------------------------------------------------------------ 编辑 / 删除

    private void editSelectedHouse() {
        House selected = getSelectedHouse("编辑");
        if (selected == null) {
            return;
        }
        showHouseDialog(selected);
    }

    private void deleteSelectedHouse() {
        House selected = getSelectedHouse("删除");
        if (selected == null) {
            return;
        }

        Object[] options = {"取消", "确认删除"};
        int choice = JOptionPane.showOptionDialog(this,
                "确定要删除房屋 " + selected.getId() + "（" + selected.getAddress()
                        + "）吗？此操作不可撤销。",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]); // 默认焦点在「取消」，避免习惯性回车误删

        if (choice != 1) {
            return;
        }

        Result result = houseController.deleteHouse(selected.getId());
        if (result.isSuccess()) {
            Toast.success(this, result.getMessage());
            refresh();
        } else {
            warn(this, result.getMessage());
        }
    }

    /**
     * 取当前选中的房屋。用 convertRowIndexToModel 换算行号，
     * 这样将来开启表头排序（G-005）也不会取错行。
     */
    private House getSelectedHouse(String action) {
        int viewRow = houseTable.getSelectedRow();
        if (viewRow == -1) {
            warn(this, "请先选择要" + action + "的房屋");
            return null;
        }

        int modelRow = houseTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentHouses.size()) {
            warn(this, "数据已发生变化，请重新选择");
            refresh();
            return null;
        }
        return currentHouses.get(modelRow);
    }

    // -------------------------------------------------------------- 新增 / 编辑对话框

    /**
     * 新增与编辑共用一个对话框。
     *
     * @param existing 为 null 表示新增；否则为编辑，此时房屋ID 只读
     */
    private void showHouseDialog(House existing) {
        final boolean editing = existing != null;

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                editing ? "编辑房屋" : "添加新房屋", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());

        JLabel title = new JLabel(editing ? "编辑房屋" : "添加新房屋");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_HEADING);
        title.setBorder(new EmptyBorder(18, 20, 0, 20));
        dialog.add(title, BorderLayout.NORTH);

        JTextField houseId = new JTextField(editing ? existing.getId() : "");
        JTextField type = new JTextField(editing ? existing.getType() : "");
        JTextField area = new JTextField(editing ? formatArea(existing.getArea()) : "");
        JTextField address = new JTextField(editing ? existing.getAddress() : "");
        JTextField landlordId = new JTextField(editing ? existing.getLandlord().getId() : "");
        JTextField landlordName = new JTextField(editing ? existing.getLandlord().getName() : "");
        JTextField landlordContact =
                new JTextField(editing ? existing.getLandlord().getContact() : "");

        if (editing) {
            // 主键不可改：改主键等于换一条记录，语义上应是「删旧增新」
            houseId.setEditable(false);
            houseId.setBackground(Theme.DISABLED_BG);
            houseId.setToolTipText("房屋ID 是主键，编辑时不可修改");
        }

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

        JButton submit = primaryButton(editing ? "保存" : "提交");
        submit.addActionListener(e -> {
            double areaValue;
            try {
                areaValue = Double.parseDouble(area.getText().trim());
            } catch (NumberFormatException ex) {
                warn(dialog, "面积必须是数字");
                area.requestFocusInWindow();
                return;
            }

            Result result = editing
                    ? houseController.updateHouse(
                            houseId.getText(), type.getText(), areaValue, address.getText(),
                            landlordId.getText(), landlordName.getText(), landlordContact.getText())
                    : houseController.addHouse(
                            houseId.getText(), type.getText(), areaValue, address.getText(),
                            landlordId.getText(), landlordName.getText(), landlordContact.getText());

            if (result.isSuccess()) {
                Toast.success(this, result.getMessage());
                refresh();
                dialog.dispose();
            } else {
                warn(dialog, result.getMessage());
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

    /** 统一的失败提示（校验不通过、ID 冲突、保存失败等） */
    private void warn(java.awt.Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "无法保存", JOptionPane.WARNING_MESSAGE);
    }

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
        styleAsSecondary(button);
        return button;
    }

    private void styleAsSecondary(JButton button) {
        button.setBackground(Theme.SURFACE);
        button.setForeground(Theme.TEXT_PRIMARY);
        button.setBorder(outlineBorder(Theme.BORDER_INPUT));
    }
}
