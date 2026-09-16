package view;

import controller.CustomerController;
import model.Customer;
import util.Result;
import util.Theme;

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
import java.awt.Component;
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

public class CustomerView extends JPanel {

    private static final String[] COLUMNS = {"ID", "姓名", "电话", "需求描述"};

    private final CustomerController customerController;
    private final Consumer<String> statusReporter;

    private final JTable customerTable;
    private final JButton editButton = new JButton("编辑客户");
    private final JButton deleteButton = new JButton("删除客户");

    /** 与表格行一一对应的数据，避免再从表格单元格里反解字段 */
    private List<Customer> currentCustomers = new ArrayList<>();

    public CustomerView(CustomerController customerController, Consumer<String> statusReporter) {
        this.customerController = customerController;
        this.statusReporter = statusReporter;

        setLayout(new BorderLayout());
        setBackground(Theme.PAGE_BG);
        setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = createHeader();
        customerTable = createCustomerTable();

        JScrollPane scrollPane = new JScrollPane(customerTable);
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

        JLabel title = new JLabel("客户信息管理");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_HEADING);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);

        JButton addButton = primaryButton("添加客户");
        addButton.addActionListener(e -> showCustomerDialog(null));

        editButton.setFont(Theme.FONT_BODY);
        editButton.setFocusPainted(false);
        editButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editButton.addActionListener(e -> editSelectedCustomer());
        styleAsSecondary(editButton);

        deleteButton.setFont(Theme.FONT_BODY);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> deleteSelectedCustomer());

        JButton refreshButton = secondaryOutlineButton("刷新数据");
        refreshButton.addActionListener(e -> refresh());

        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        buttons.add(refreshButton);

        top.add(title, BorderLayout.NORTH);
        top.add(buttons, BorderLayout.SOUTH);
        return top;
    }

    private JTable createCustomerTable() {
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
        currentCustomers = customerController.getAllCustomers();

        DefaultTableModel model = (DefaultTableModel) customerTable.getModel();
        model.setRowCount(0);
        for (Customer customer : currentCustomers) {
            model.addRow(new Object[]{
                    customer.getId(),
                    customer.getName(),
                    customer.getPhone(),
                    customer.getRequirements()
            });
        }

        if (statusReporter != null) {
            statusReporter.accept("共 " + currentCustomers.size() + " 条客户记录");
        }
    }

    // ------------------------------------------------------------ 权限控制

    /**
     * 按当前用户权限启用或置灰「删除客户」按钮。
     *
     * <p><b>登录成功后必须由 MainView 再次调用本方法。</b>本视图是在登录之前就被
     * 构造的，那时 Session 里还没有用户，按钮必然是禁用态。
     *
     * <p>界面层置灰只是体验优化——真正的防护在 CustomerController.deleteCustomer。
     */
    public void applyPermissions() {
        boolean allowed = customerController.canDelete();
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

    private void editSelectedCustomer() {
        Customer selected = getSelectedCustomer("编辑");
        if (selected == null) {
            return;
        }
        showCustomerDialog(selected);
    }

    private void deleteSelectedCustomer() {
        Customer selected = getSelectedCustomer("删除");
        if (selected == null) {
            return;
        }

        Object[] options = {"取消", "确认删除"};
        int choice = JOptionPane.showOptionDialog(this,
                "确定要删除客户 " + selected.getName() + "（ID: " + selected.getId()
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

        Result result = customerController.deleteCustomer(selected.getId());
        if (result.isSuccess()) {
            Toast.success(this, result.getMessage());
            refresh();
        } else {
            warn(this, result.getMessage());
        }
    }

    /**
     * 取当前选中的客户。用 convertRowIndexToModel 换算行号，
     * 这样将来开启表头排序（G-005）也不会取错行。
     */
    private Customer getSelectedCustomer(String action) {
        int viewRow = customerTable.getSelectedRow();
        if (viewRow == -1) {
            warn(this, "请先选择要" + action + "的客户");
            return null;
        }

        int modelRow = customerTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentCustomers.size()) {
            warn(this, "数据已发生变化，请重新选择");
            refresh();
            return null;
        }
        return currentCustomers.get(modelRow);
    }

    // -------------------------------------------------------------- 新增 / 编辑对话框

    /**
     * 新增与编辑共用一个对话框。
     *
     * @param existing 为 null 表示新增；否则为编辑，此时客户ID 只读
     */
    private void showCustomerDialog(Customer existing) {
        final boolean editing = existing != null;

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                editing ? "编辑客户" : "添加新客户", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());

        JLabel title = new JLabel(editing ? "编辑客户" : "添加新客户");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_HEADING);
        title.setBorder(new EmptyBorder(18, 20, 0, 20));
        dialog.add(title, BorderLayout.NORTH);

        JTextField customerId = new JTextField(editing ? existing.getId() : "");
        JTextField name = new JTextField(editing ? existing.getName() : "");
        JTextField phone = new JTextField(editing ? existing.getPhone() : "");
        JTextField requirements = new JTextField(editing ? existing.getRequirements() : "");

        if (editing) {
            // 主键不可改：改主键等于换一条记录，语义上应是「删旧增新」
            customerId.setEditable(false);
            customerId.setBackground(Theme.DISABLED_BG);
            customerId.setToolTipText("客户ID 是主键，编辑时不可修改");
        }

        // 客户只有 4 个字段，语义一致，无需像房屋那样分组
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 20, 0, 20));
        body.add(twoColumnForm(
                new String[]{"客户ID", "姓名", "电话", "需求描述"},
                new JTextField[]{customerId, name, phone, requirements}));

        dialog.add(body, BorderLayout.CENTER);

        JButton cancel = secondaryOutlineButton("取消");
        cancel.addActionListener(e -> dialog.dispose());

        JButton submit = primaryButton(editing ? "保存" : "提交");
        submit.addActionListener(e -> {
            Result result = editing
                    ? customerController.updateCustomer(customerId.getText(), name.getText(),
                            phone.getText(), requirements.getText())
                    : customerController.addCustomer(customerId.getText(), name.getText(),
                            phone.getText(), requirements.getText());

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
    private void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "无法保存", JOptionPane.WARNING_MESSAGE);
    }

    /** 两列排布的表单：每行两组「标签 + 输入框」 */
    private JPanel twoColumnForm(String[] labels, JTextField[] fields) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);

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
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void styleAsSecondary(JButton button) {
        button.setBackground(Theme.SURFACE);
        button.setForeground(Theme.TEXT_PRIMARY);
        button.setBorder(outlineBorder(Theme.BORDER_INPUT));
    }
}
