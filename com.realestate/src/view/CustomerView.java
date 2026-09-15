package view;


import controller.CustomerController;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
        import java.awt.event.ActionEvent;
import java.util.List;

public class CustomerView extends JPanel {
    private final CustomerController customerController;
    private final JTable customerTable;

    public CustomerView(CustomerController customerController) {
        this.customerController = customerController;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initializeUI();

        customerTable = createCustomerTable();
        add(new JScrollPane(customerTable), BorderLayout.CENTER);
        loadCustomerData();
    }

    private void initializeUI() {
        // 顶部面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // 标题
        JLabel titleLabel = new JLabel("客户信息管理", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        titleLabel.setForeground(new Color(25, 25, 112));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        // 添加客户按钮
        JButton addButton = new JButton("添加客户");
        addButton.addActionListener(this::showAddCustomerDialog);

        // 删除客户按钮
        JButton deleteButton = new JButton("删除客户");
        deleteButton.addActionListener(e -> deleteSelectedCustomer());

        // 刷新按钮
        JButton refreshButton = new JButton("刷新数据");
        refreshButton.addActionListener(e -> loadCustomerData());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);

        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);
    }

    private JTable createCustomerTable() {
        String[] columns = {"ID", "姓名", "电话", "需求描述"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 14));
        return table;
    }

    private void loadCustomerData() {
        DefaultTableModel model = (DefaultTableModel) customerTable.getModel();
        model.setRowCount(0);

        List<Object[]> customers = customerController.getAllCustomers();
        for (Object[] customer : customers) {
            model.addRow(customer);
        }
    }

    private void showAddCustomerDialog(ActionEvent e) {
        // 创建对话框
        JDialog dialog = new JDialog();
        dialog.setTitle("添加新客户");
        dialog.setModal(true);
        dialog.setSize(400, 300);
        dialog.setLayout(new GridLayout(0, 2, 10, 10)); // 网格布局，每行两个组件

        // 表单字段
        JLabel lblCustomerId = new JLabel("客户ID:");
        JTextField txtCustomerId = new JTextField(20);
        JLabel lblName = new JLabel("姓名:");
        JTextField txtName = new JTextField(20);
        JLabel lblPhone = new JLabel("电话:");
        JTextField txtPhone = new JTextField(20);
        JLabel lblRequirements = new JLabel("需求:");
        JTextField txtRequirements = new JTextField(20);

        // 提交按钮
        JButton btnSubmit = new JButton("提交");
        btnSubmit.addActionListener(evt -> {
            try {
                // 获取输入的值
                String customerId = txtCustomerId.getText();
                String name = txtName.getText();
                String phone = txtPhone.getText();
                String requirements = txtRequirements.getText();

                // 调用控制器添加客户
                boolean success = customerController.addCustomer(customerId, name, phone, requirements);

                if (success) {
                    JOptionPane.showMessageDialog(dialog, "客户添加成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                    loadCustomerData(); // 刷新表格数据
                    dialog.dispose(); // 关闭对话框
                } else {
                    JOptionPane.showMessageDialog(dialog, "客户添加失败！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "添加客户时出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 取消按钮
        JButton btnCancel = new JButton("取消");
        btnCancel.addActionListener(evt -> dialog.dispose());

        // 添加组件到对话框
        dialog.add(lblCustomerId);
        dialog.add(txtCustomerId);
        dialog.add(lblName);
        dialog.add(txtName);
        dialog.add(lblPhone);
        dialog.add(txtPhone);
        dialog.add(lblRequirements);
        dialog.add(txtRequirements);
        dialog.add(btnSubmit);
        dialog.add(btnCancel);

        // 显示对话框
        dialog.setLocationRelativeTo(null); // 居中显示
        dialog.setVisible(true);
    }

    private void deleteSelectedCustomer() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先选择要删除的客户", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String customerId = (String) customerTable.getValueAt(selectedRow, 0);
        String customerName = (String) customerTable.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "确定要删除客户 " + customerName + " (ID: " + customerId + ") 吗？",
                "确认删除",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (customerController.deleteCustomer(customerId)) {
                JOptionPane.showMessageDialog(this, "客户删除成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
                loadCustomerData();
            } else {
                JOptionPane.showMessageDialog(this, "删除客户失败", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}