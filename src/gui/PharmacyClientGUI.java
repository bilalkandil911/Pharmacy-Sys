package gui;

import model.Medicine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PharmacyClientGUI extends JFrame {

    private static final String HOST = "localhost";
    private static final int    PORT = 5000;

    private ObjectOutputStream out;
    private ObjectInputStream  in;

    private JTable            inventoryTable;
    private DefaultTableModel inventoryModel;

    private JTable            cartTable;
    private DefaultTableModel cartModel;

    private final Map<String, CartItem> cart = new LinkedHashMap<>();

    private JTextField qtyField;
    private JCheckBox  prescriptionBox;
    private JButton    addToCartBtn;
    private JButton    removeFromCartBtn;
    private JButton    checkoutBtn;
    private JLabel     statusLabel;
    private JLabel     cartTotalLabel;

    // Cart
    private static class CartItem {
        String  name;
        int     qty;
        double  unitPrice;
        double  discount;
        boolean needsPrescription;
        boolean hasPrescription;

        CartItem(String name, int qty, double unitPrice, double discount,
                 boolean needsPrescription, boolean hasPrescription) {
            this.name              = name;
            this.qty               = qty;
            this.unitPrice         = unitPrice;
            this.discount          = discount;
            this.needsPrescription = needsPrescription;
            this.hasPrescription   = hasPrescription;
        }

        double lineTotal() { return unitPrice * qty; }
    }

    // ---------------------------------------------------------------
    public PharmacyClientGUI() {
        super("Pharmacy Management System");
        buildUI();
        connectToServer();
    }

    // ---------------------------------------------------------------
    // UI
    // ---------------------------------------------------------------
    private void buildUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(12, 16, 8, 16));
        JLabel title = new JLabel("Pharmacy Branch Client");
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildInventoryPanel(), buildCartPanel());
        split.setDividerLocation(640);
        split.setResizeWeight(0.6);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        add(buildBottomBar(), BorderLayout.SOUTH);
    }

    private JPanel buildInventoryPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBorder(new EmptyBorder(0, 8, 0, 4));

        JLabel lbl = new JLabel("Available Medicines");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setBorder(new EmptyBorder(6, 2, 4, 0));
        p.add(lbl, BorderLayout.NORTH);

        String[] cols = {"Name", "Type", "Manufacturer", "Price", "Final", "Disc.", "Stock", "Rx"};
        inventoryModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        inventoryTable = new JTable(inventoryModel);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inventoryTable.getTableHeader().setReorderingAllowed(false);
        inventoryTable.setRowHeight(24);
        styleColumns(inventoryTable, new int[]{180, 45, 110, 60, 60, 45, 45, 110});

        inventoryTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                                                           boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    String rx = inventoryModel.getValueAt(row, 7).toString();
                    c.setBackground(rx.startsWith("Required")
                            ? new Color(255, 253, 230) : Color.WHITE);
                }
                return c;
            }
        });

        p.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCartPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBorder(new EmptyBorder(0, 4, 0, 8));

        JPanel top = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel("Cart");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setBorder(new EmptyBorder(6, 2, 4, 0));
        top.add(lbl, BorderLayout.WEST);

        cartTotalLabel = new JLabel("Total: $0.00");
        cartTotalLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        cartTotalLabel.setForeground(new Color(0, 100, 50));
        top.add(cartTotalLabel, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        String[] cols = {"Medicine", "Qty", "Unit ($)", "Line Total ($)", "Rx"};
        cartModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.getTableHeader().setReorderingAllowed(false);
        cartTable.setRowHeight(24);
        styleColumns(cartTable, new int[]{160, 40, 70, 90, 90});

        p.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                new EmptyBorder(2, 6, 2, 6)));

        bar.add(new JLabel("Qty:"));
        qtyField = new JTextField(4);
        bar.add(qtyField);

        prescriptionBox = new JCheckBox("Has prescription");
        bar.add(prescriptionBox);

        addToCartBtn = new JButton("Add to Cart +");
        addToCartBtn.setEnabled(false);
        addToCartBtn.addActionListener(e -> addToCart());
        bar.add(addToCartBtn);

        removeFromCartBtn = new JButton("Remove Selected");
        removeFromCartBtn.setEnabled(false);
        removeFromCartBtn.addActionListener(e -> removeFromCart());
        bar.add(removeFromCartBtn);

        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 28));
        bar.add(sep);

        checkoutBtn = new JButton("Place Order");
        checkoutBtn.setEnabled(false);
        checkoutBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        checkoutBtn.setForeground(new Color(0, 100, 40));
        checkoutBtn.addActionListener(e -> checkout());
        bar.add(checkoutBtn);

        statusLabel = new JLabel("Connecting to server...");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        bar.add(statusLabel);

        cartTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                removeFromCartBtn.setEnabled(cartTable.getSelectedRow() >= 0);
        });

        return bar;
    }

    // Connect and load
    @SuppressWarnings("unchecked")
    private void connectToServer() {
        new SwingWorker<List<Medicine>, Void>() {
            @Override protected List<Medicine> doInBackground() throws Exception {
                Socket socket = new Socket(HOST, PORT);
                out = new ObjectOutputStream(socket.getOutputStream());
                in  = new ObjectInputStream(socket.getInputStream());
                return (List<Medicine>) in.readObject();
            }
            @Override protected void done() {
                try {
                    List<Medicine> inv = get();
                    populateInventory(inv);
                    setStatus("Connected — " + inv.size() + " medicines loaded.", false);
                    addToCartBtn.setEnabled(true);
                } catch (Exception e) {
                    setStatus("Cannot connect to server. Is PharmacyServer running?", true);
                }
            }
        }.execute();
    }

    private void populateInventory(List<Medicine> inventory) {
        inventoryModel.setRowCount(0);
        for (Medicine m : inventory) {
            inventoryModel.addRow(new Object[]{
                    m.getName(), m.getType(), m.getManufacturer(),
                    String.format("%.2f", m.getPrice()),
                    String.format("%.2f", m.getFinalPrice()),
                    String.format("%.0f%%", m.getDiscountRate() * 100),
                    m.getStock(),
                    m.requiresPrescription()
                            ? "Required (" + m.getPrescriptionLevel() + ")" : "Not required"
            });
        }
    }

    // Cart operations
    private void addToCart() {
        int row = inventoryTable.getSelectedRow();
        if (row < 0) { setStatus("Select a medicine from the list first.", true); return; }

        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            setStatus("Enter a valid positive quantity.", true);
            return;
        }

        String  name      = inventoryModel.getValueAt(row, 0).toString();
        int     stock     = Integer.parseInt(inventoryModel.getValueAt(row, 6).toString());
        boolean needsRx   = inventoryModel.getValueAt(row, 7).toString().startsWith("Required");
        boolean hasRx     = prescriptionBox.isSelected();
        double  unitPrice = Double.parseDouble(inventoryModel.getValueAt(row, 4).toString());
        double  discount  = Double.parseDouble(
                inventoryModel.getValueAt(row, 5).toString().replace("%", "")) / 100.0;

        if (qty > stock) {
            setStatus("Only " + stock + " units available in stock.", true);
            return;
        }
        if (needsRx && !hasRx) {
            setStatus("\"" + name + "\" requires a prescription — tick the checkbox.", true);
            return;
        }

        if (cart.containsKey(name)) {
            CartItem existing = cart.get(name);
            int newQty = existing.qty + qty;
            if (newQty > stock) {
                setStatus("Total cart qty (" + newQty + ") exceeds stock (" + stock + ").", true);
                return;
            }
            existing.qty = newQty;
        } else {
            cart.put(name, new CartItem(name, qty, unitPrice, discount, needsRx, hasRx));
        }

        refreshCartTable();
        qtyField.setText("");
        prescriptionBox.setSelected(false);
        setStatus("\"" + name + "\" added to cart.", false);
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row < 0) return;
        String name = cartModel.getValueAt(row, 0).toString();
        cart.remove(name);
        refreshCartTable();
        removeFromCartBtn.setEnabled(false);
        setStatus("\"" + name + "\" removed from cart.", false);
    }

    private void refreshCartTable() {
        cartModel.setRowCount(0);
        double total = 0;
        for (CartItem item : cart.values()) {
            cartModel.addRow(new Object[]{
                    item.name, item.qty,
                    String.format("%.2f", item.unitPrice),
                    String.format("%.2f", item.lineTotal()),
                    item.needsPrescription ? "Rx" : "—"
            });
            total += item.lineTotal();
        }
        cartTotalLabel.setText(String.format("Total: $%.2f", total));
        checkoutBtn.setEnabled(!cart.isEmpty());
    }

    // Checkout — ONE write of the whole cart, ONE read of all results
    private void checkout() {
        if (cart.isEmpty()) return;

        checkoutBtn.setEnabled(false);
        addToCartBtn.setEnabled(false);
        setStatus("Sending order to server...", false);

        // Snapshot the cart into a plain list
        final List<CartItem> items = new ArrayList<>(cart.values());

        // Build a String[][] where each row is [name, qty, hasPrescription]
        // This entire array is sent in ONE writeObject call
        final String[][] payload = new String[items.size()][3];
        for (int i = 0; i < items.size(); i++) {
            CartItem it = items.get(i);
            payload[i][0] = it.name;
            payload[i][1] = String.valueOf(it.qty);
            payload[i][2] = it.hasPrescription ? "yes" : "no";
        }

        new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() throws Exception {
                // ── Single send: entire cart in one message ──
                out.writeObject(payload);
                out.flush();

                // ── Single receive: server returns all results in one message ──
                return (String[]) in.readObject();
            }

            @Override
            protected void done() {
                try {
                    String[] responses = get();
                    showReceipt(items, responses);

                    // Clear only successfully processed items
                    for (int i = 0; i < items.size(); i++) {
                        if (!responses[i].startsWith("ERROR"))
                            cart.remove(items.get(i).name);
                    }
                    refreshCartTable();
                    setStatus("Order complete. See receipt.", false);
                } catch (Exception e) {
                    setStatus("Lost connection to server.", true);
                } finally {
                    checkoutBtn.setEnabled(!cart.isEmpty());
                    addToCartBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    // Receipt dialog
    private void showReceipt(List<CartItem> items, String[] responses) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("==============================================\n");
        sb.append("       PHARMACY MANAGEMENT SYSTEM\n");
        sb.append("                 RECEIPT\n");
        sb.append("==============================================\n");
        sb.append(String.format("  Date/Time : %s%n", timestamp));
        sb.append("  --------------------------------------------\n");
        sb.append(String.format("  %-22s %4s  %8s  %9s%n",
                "Medicine", "Qty", "Unit ($)", "Total ($)"));
        sb.append("  --------------------------------------------\n");

        double grandTotal   = 0;
        double totalSavings = 0;
        int    successCount = 0;
        boolean hasErrors   = false;

        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            String   resp = responses[i];

            if (resp.startsWith("ERROR")) {
                sb.append(String.format("  [FAILED] %s%n", item.name));
                sb.append(String.format("           %s%n", resp.replace("ERROR: ", "")));
                hasErrors = true;
            } else {
                double lineTotal = item.lineTotal();
                double saving    = (item.unitPrice / (1.0 - item.discount))
                        * item.discount * item.qty;
                grandTotal   += lineTotal;
                totalSavings += saving;
                successCount++;
                sb.append(String.format("  [OK]  %-22s %4d  %8.2f  %9.2f%n",
                        item.name, item.qty, item.unitPrice, lineTotal));
                if (item.discount > 0)
                    sb.append(String.format("        %.0f%% discount — you saved $%.2f%n",
                            item.discount * 100, saving));
            }
        }

        sb.append("  --------------------------------------------\n");
        sb.append(String.format("  Items processed : %d / %d%n", successCount, items.size()));
        if (totalSavings > 0)
            sb.append(String.format("  Total savings   : $%.2f%n", totalSavings));
        sb.append(String.format("  GRAND TOTAL     : $%.2f%n", grandTotal));
        sb.append("  ============================================\n");
        if (hasErrors)
            sb.append("  Note: some items failed (see [FAILED] above).\n");
        sb.append("        Thank you for your order!\n");

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        area.setBackground(new Color(252, 252, 248));
        area.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(520, 420));

        JOptionPane.showMessageDialog(this, scroll,
                "Order Receipt", JOptionPane.INFORMATION_MESSAGE);
    }

    // Helpers
    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setForeground(isError ? Color.RED : new Color(0, 110, 50));
    }

    private void styleColumns(JTable t, int[] widths) {
        for (int i = 0; i < widths.length && i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PharmacyClientGUI().setVisible(true));
    }
}