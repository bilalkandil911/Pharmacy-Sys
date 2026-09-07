package server;

import model.Medicine;
import util.FileManager;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PharmacyServer {

    private static final int PORT = 5000;
    private static List<Medicine> inventory;
    private static FileManager fileManager;

    private static int clientCounter = 0;

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private static String now() {
        return LocalDateTime.now().format(TS);
    }

    private static final String THICK = "  ══════════════════════════════════════════════════════";
    private static final String THIN  = "  ──────────────────────────────────────────────────────";


    public static void main(String[] args) {
        fileManager = new FileManager("inventory.csv");
        inventory   = fileManager.loadInventory();

        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║          PHARMACY MANAGEMENT SYSTEM — SERVER           ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.printf("[%s] Inventory loaded: %d medicines%n", now(), inventory.size());
        System.out.printf("[%s] Listening on port %d ...%n%n", now(), PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientId = ++clientCounter;
                System.out.printf("[%s] ► Client #%d connected  (%s)%n",
                        now(), clientId, clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket, clientId)).start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Could not start: " + e.getMessage());
        }
    }

    // Helper one processed order line kept for the summary
    static class OrderLine {
        final String  medicineName;
        final int     qty;
        final boolean approved;
        final String  response;  // full response string sent back to client

        OrderLine(String medicineName, int qty, boolean approved, String response) {
            this.medicineName = medicineName;
            this.qty          = qty;
            this.approved     = approved;
            this.response     = response;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ClientHandler
    static class ClientHandler implements Runnable {

        private final Socket socket;
        private final int    clientId;

        ClientHandler(Socket socket, int clientId) {
            this.socket   = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

                // Sending inventory
                out.writeObject(inventory);
                out.flush();
                System.out.printf("[%s] Client #%d — inventory sent (%d items)%n",
                        now(), clientId, inventory.size());

                // ── Wait for ONE message: the full cart as String[][]
                Object obj = in.readObject();

                if (obj instanceof String[][] cartPayload) {

                    System.out.printf("[%s] Client #%d — ORDER RECEIVED (%d items in cart)%n",
                            now(), clientId, cartPayload.length);

                    // Process every line and collect results
                    List<OrderLine> log      = new ArrayList<>();
                    String[]        responses = new String[cartPayload.length];

                    for (int i = 0; i < cartPayload.length; i++) {
                        String name  = cartPayload[i][0];
                        String qty   = cartPayload[i][1];
                        String rx    = cartPayload[i][2];

                        String result = processOrder(name, qty, rx);
                        responses[i]  = result;

                        boolean ok = result.startsWith("SUCCESS");
                        log.add(new OrderLine(name, safeInt(qty), ok, result));
                    }

                    // ── Send ALL results back in ONE message
                    out.writeObject(responses);
                    out.flush();

                    // ── Print the order summary and stock report
                    printOrderSummary(log);
                    printStockReport(log);
                }

            } catch (EOFException | SocketException e) {
                System.out.printf("[%s] ◄ Client #%d disconnected%n%n", now(), clientId);
            } catch (IOException | ClassNotFoundException e) {
                System.err.printf("[%s] Client #%d error: %s%n", now(), clientId, e.getMessage());
            }
        }

        // Full order summary
        private void printOrderSummary(List<OrderLine> log) {
            System.out.println();
            System.out.println(THICK);
            System.out.printf("  ORDER SUMMARY — Client #%d   [%s]%n", clientId, now());
            System.out.println(THICK);
            System.out.printf("  %-28s  %5s  %-10s  %s%n",
                    "Medicine", "Qty", "Status", "Details");
            System.out.println(THIN);

            int    approved   = 0;
            int    rejected   = 0;
            double grandTotal = 0;

            for (OrderLine line : log) {
                String status = line.approved ? "✔ OK      " : "✘ REJECTED";

                String detail;
                if (line.approved) {
                    // Show just the Unit price + Total + Discount part
                    detail = line.response.replaceFirst(
                            "SUCCESS: Ordered \\d+ x .+? \\| ", "");
                    grandTotal += parseTotal(line.response);
                    approved++;
                } else {
                    detail = line.response.replace("ERROR: ", "");
                    rejected++;
                }

                System.out.printf("  %-28s  %5d  %s  %s%n",
                        line.medicineName, line.qty, status, detail);
            }

            System.out.println(THIN);
            System.out.printf("  Items approved : %d%n", approved);
            System.out.printf("  Items rejected : %d%n", rejected);
            if (grandTotal > 0)
                System.out.printf("  Grand total    : $%.2f%n", grandTotal);
            System.out.println(THICK);
        }

        // ── Stock report — only for medicines whose stock changed ──
        private void printStockReport(List<OrderLine> log) {
            boolean anyApproved = log.stream().anyMatch(l -> l.approved);
            if (!anyApproved) return;

            System.out.println();
            System.out.printf("  STOCK UPDATE after Client #%d order:%n", clientId);
            System.out.println(THIN);

            for (OrderLine line : log) {
                if (!line.approved) continue;
                for (Medicine m : inventory) {
                    if (m.getName().equalsIgnoreCase(line.medicineName)) {
                        System.out.printf("  %-28s  →  %d units remaining%n",
                                m.getName(), m.getStock());
                        break;
                    }
                }
            }

            System.out.println(THIN);
            System.out.println();
        }

        // Order processing
        private synchronized String processOrder(String name, String qtyStr,
                                                 String hasPrescription) {
            int qty;
            try {
                qty = Integer.parseInt(qtyStr.trim());
                if (qty <= 0) return "ERROR: Quantity must be a positive number.";
            } catch (NumberFormatException e) {
                return "ERROR: Invalid quantity. Please enter a whole number.";
            }

            Medicine target = null;
            for (Medicine m : inventory) {
                if (m.getName().equalsIgnoreCase(name.trim())) { target = m; break; }
            }
            if (target == null) return "ERROR: Medicine \"" + name + "\" not found.";

            if (target.requiresPrescription() && !hasPrescription.equalsIgnoreCase("yes"))
                return "ERROR: \"" + name + "\" requires a prescription. Order rejected.";

            if (target.getStock() < qty)
                return "ERROR: Insufficient stock. Available: " + target.getStock();

            target.setStock(target.getStock() - qty);
            double total = target.getFinalPrice() * qty;
            fileManager.saveInventory(inventory);

            return String.format(
                    "SUCCESS: Ordered %d x %s | Unit price: $%.2f | Total: $%.2f | Discount: %.0f%%",
                    qty, target.getName(), target.getFinalPrice(), total,
                    target.getDiscountRate() * 100);
        }

        // ── Parse "Total: $Y" from a SUCCESS response ──
        private double parseTotal(String response) {
            try {
                for (String part : response.split("\\|")) {
                    part = part.trim();
                    if (part.startsWith("Total:"))
                        return Double.parseDouble(part.replace("Total: $", "").trim());
                }
            } catch (NumberFormatException ignored) {}
            return 0;
        }

        private int safeInt(String s) {
            try { return Integer.parseInt(s.trim()); }
            catch (NumberFormatException e) { return 0; }
        }
    }
}