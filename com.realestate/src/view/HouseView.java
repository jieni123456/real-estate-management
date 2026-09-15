package view;


import controller.HouseController;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
        import java.awt.event.ActionEvent;
import java.util.List;

public class HouseView extends JPanel {
    private final HouseController houseController;
    private final JTable houseTable;

    public HouseView(HouseController houseController) {
        this.houseController = houseController;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initializeUI();

        houseTable = createHouseTable();
        add(new JScrollPane(houseTable), BorderLayout.CENTER);
        loadHouseData();
    }

    private void initializeUI() {
        // 顶部面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // 标题
        JLabel titleLabel = new JLabel("房屋信息管理", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        titleLabel.setForeground(new Color(25, 25, 112));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        // 添加房屋按钮
        JButton addButton = new JButton("添加房屋");
        addButton.addActionListener(this::showAddHouseDialog);

        // 删除房屋按钮
        JButton deleteButton = new JButton("删除房屋");
        deleteButton.addActionListener(e -> deleteSelectedHouse());

        // 刷新按钮
        JButton refreshButton = new JButton("刷新数据");
        refreshButton.addActionListener(e -> loadHouseData());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);

        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);
    }

    private JTable createHouseTable() {
        String[] columns = {"ID", "户型", "面积(m²)", "地址", "房东ID", "房东姓名", "房东电话"};
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

    private void loadHouseData() {
        DefaultTableModel model = (DefaultTableModel) houseTable.getModel();
        model.setRowCount(0);

        List<Object[]> houses = houseController.getAllHouses();
        for (Object[] house : houses) {
            model.addRow(house);
        }
    }

    private void showAddHouseDialog(ActionEvent e) {
        // 创建对话框
        JDialog dialog = new JDialog();
        dialog.setTitle("添加新房屋");
        dialog.setModal(true);
        dialog.setSize(500, 450);
        dialog.setLayout(new GridLayout(0, 2, 10, 10)); // 网格布局，每行两个组件

        // 表单字段
        JLabel lblHouseId = new JLabel("房屋ID:");
        JTextField txtHouseId = new JTextField(20);
        JLabel lblType = new JLabel("户型:");
        JTextField txtType = new JTextField(20);
        JLabel lblArea = new JLabel("面积(m²):");
        JTextField txtArea = new JTextField(20);
        JLabel lblAddress = new JLabel("地址:");
        JTextField txtAddress = new JTextField(20);
        JLabel lblLandlordId = new JLabel("房东ID:");
        JTextField txtLandlordId = new JTextField(20);
        JLabel lblLandlordName = new JLabel("房东姓名:");
        JTextField txtLandlordName = new JTextField(20);
        JLabel lblLandlordContact = new JLabel("房东电话:");
        JTextField txtLandlordContact = new JTextField(20);

        // 提交按钮
        JButton btnSubmit = new JButton("提交");
        btnSubmit.addActionListener(evt -> {
            try {
                // 获取输入的值
                String houseId = txtHouseId.getText();
                String type = txtType.getText();
                double area = Double.parseDouble(txtArea.getText());
                String address = txtAddress.getText();
                String landlordId = txtLandlordId.getText();
                String landlordName = txtLandlordName.getText();
                String landlordContact = txtLandlordContact.getText();

                // 调用控制器添加房屋
                boolean success = houseController.addHouse(
                        houseId, type, area, address,
                        landlordId, landlordName, landlordContact
                );

                if (success) {
                    JOptionPane.showMessageDialog(dialog, "房屋添加成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                    loadHouseData(); // 刷新表格数据
                    dialog.dispose(); // 关闭对话框
                } else {
                    JOptionPane.showMessageDialog(dialog, "房屋添加失败！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "面积必须是数字！", "输入错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 取消按钮
        JButton btnCancel = new JButton("取消");
        btnCancel.addActionListener(evt -> dialog.dispose());

        // 添加组件到对话框
        dialog.add(lblHouseId);
        dialog.add(txtHouseId);
        dialog.add(lblType);
        dialog.add(txtType);
        dialog.add(lblArea);
        dialog.add(txtArea);
        dialog.add(lblAddress);
        dialog.add(txtAddress);
        dialog.add(lblLandlordId);
        dialog.add(txtLandlordId);
        dialog.add(lblLandlordName);
        dialog.add(txtLandlordName);
        dialog.add(lblLandlordContact);
        dialog.add(txtLandlordContact);
        dialog.add(btnSubmit);
        dialog.add(btnCancel);

        // 显示对话框
        dialog.setLocationRelativeTo(null); // 居中显示
        dialog.setVisible(true);
    }

    private void deleteSelectedHouse() {
        int selectedRow = houseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先选择要删除的房屋", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String houseId = (String) houseTable.getValueAt(selectedRow, 0);
        String address = (String) houseTable.getValueAt(selectedRow, 3);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "确定要删除房屋 " + houseId + " (" + address + ") 吗？",
                "确认删除",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (houseController.deleteHouse(houseId)) {
                JOptionPane.showMessageDialog(this, "房屋删除成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
                loadHouseData();
            } else {
                JOptionPane.showMessageDialog(this, "删除房屋失败", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}