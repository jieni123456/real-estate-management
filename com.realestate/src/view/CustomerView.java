package view;

import controller.CustomerController;
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

public class CustomerView extends JPanel {

    private static final String[] COLUMNS = {"ID", "姓名", "电话", "需求描述"};

    private final CustomerController customerController;
    private final Consumer<String> statusReporter;

    private final JTable customerTable;
    private final JButton deleteButton = new JButton("删除客户");

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

        applyDeletePermission();
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
        addButton.addActionListener(e -> showAddCustomerDialog());

        deleteButton.setFont(Theme.FONT_BODY);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> deleteSelectedCustomer());

        JButton refreshButton = secondaryOutlineButton("刷新数据");
        refreshButton.addActionListener(e -> refresh());

        buttons.add(addButton);
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
        DefaultTableModel model = (DefaultTableModel) customerTable.getModel();
        model.setRowCount(0);

        List<Object[]> customers = customerController.getAllCustomers();
        for (Object[] customer : customers) {
            model.addRow(customer);
        }

        if (statusReporter != null) {
            statusReporter.accept("共 " + customers.size() + " 条客户记录");
        }
    }

    // ------------------------------------------------------------ 权限控制

    /**
     * 按当前用户权限启用或置灰「删除客户」按钮。
     * 界面层置灰只是体验优化——真正的防护在 CustomerController.deleteCustomer。
     */
    private void applyDeletePermission() {
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

    // ------------------------------------------------------------ 删除逻辑

    private void deleteSelectedCustomer() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先选择要删除的客户", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String customerId = String.valueOf(customerTable.getValueAt(selectedRow, 0));
        String customerName = String.valueOf(customerTable.getValueAt(selectedRow, 1));

        Object[] options = {"取消", "确认删除"};
        int choice = JOptionPane.showOptionDialog(this,
                "确定要删除客户 " + customerName + "（ID: " + customerId + "）吗？此操作不可撤销。",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]); // 默认焦点在「取消」，避免习惯性回车误删

        if (choice != 1) {
            return;
        }

        if (customerController.deleteCustomer(customerId)) {
            Toast.success(this, "客户删除成功");
            refresh();
        } else {
            JOptionPane.showMessageDialog(this, "删除客户失败", "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------- 添加对话框

    private void showAddCustomerDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "添加新客户",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());

        JLabel title = new JLabel("添加新客户");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_HEADING);
        title.setBorder(new EmptyBorder(18, 20, 0, 20));
        dialog.add(title, BorderLayout.NORTH);

        JTextField customerId = new JTextField();
        JTextField name = new JTextField();
        JTextField phone = new JTextField();
        JTextField requirements = new JTextField();

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

        JButton submit = primaryButton("提交");
        submit.addActionListener(e -> {
            if (customerId.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "客户ID不能为空", "输入错误",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = customerController.addCustomer(
                    customerId.getText().trim(),
                    name.getText().trim(),
                    phone.getText().trim(),
                    requirements.getText().trim());

            if (success) {
                Toast.success(this, "客户添加成功");
                refresh();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "客户添加失败", "错误",
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
        button.setBackground(Theme.SURFACE);
        button.setForeground(Theme.TEXT_PRIMARY);
        button.setBorder(outlineBorder(Theme.BORDER_INPUT));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
