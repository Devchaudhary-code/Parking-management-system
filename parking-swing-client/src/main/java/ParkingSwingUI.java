import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ParkingSwingUI extends JFrame {

    // ---- Backend ----
    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient http = HttpClient.newHttpClient();

    // ---- UI State ----
    private final JTextField plateField = new JTextField();
    private final JComboBox<String> vehicleTypeBox =
            new JComboBox<>(new String[]{"CAR", "BIKE", "TRUCK", "OTHER"});

    private final JLabel liveCountLabel = new JLabel("0");
    private final JLabel historyCountLabel = new JLabel("0");
    private final JLabel serverStatusLabel = new JLabel("Server: unknown");

    private final DefaultTableModel liveModel = new DefaultTableModel(
            new String[]{"TicketId", "Plate", "Type", "Entry Time", "Lot", "Slot", "Status"}, 0
    ) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[]{"TicketId", "Plate", "Type", "Entry Time", "Exit Time", "Amount", "Lot", "Slot", "Status"}, 0
    ) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JTable liveTable = new JTable(liveModel);
    private final JTable historyTable = new JTable(historyModel);

    private final JTextArea logArea = new JTextArea();

    public ParkingSwingUI() {
        super("Parking System • Swing Client");

        // System look & feel (native)
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildBottomLog(), BorderLayout.SOUTH);

        styleTable(liveTable);
        styleTable(historyTable);

        // Initial load
        pingServer();
        refreshLive();
        refreshHistory();
    }

    // ---------------- UI BUILDERS ----------------

    private JComponent buildTopBar() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(14, 16, 10, 16));

        JLabel title = new JLabel("Parking Management");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        JLabel subtitle = new JLabel("Swing client → Spring Boot API → MySQL");
        subtitle.setForeground(new Color(90, 90, 90));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitle);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        serverStatusLabel.setOpaque(true);
        serverStatusLabel.setBorder(new EmptyBorder(6, 10, 6, 10));
        serverStatusLabel.setBackground(new Color(245, 245, 245));

        JButton pingBtn = softButton("Ping Server");
        pingBtn.addActionListener(e -> pingServer());

        right.add(serverStatusLabel);
        right.add(pingBtn);

        root.add(left, BorderLayout.WEST);
        root.add(right, BorderLayout.EAST);

        return root;
    }

    private JComponent buildCenter() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(0, 16, 12, 16));

        // “Card” action panel
        root.add(buildActionsCard(), BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Live (OPEN)", buildLiveTab());
        tabs.addTab("History (CLOSED)", buildHistoryTab());

        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    private JComponent buildActionsCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(12, 12, 12, 12)
        ));
        card.setBackground(Color.WHITE);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;

        // Plate
        gc.gridx = 0; gc.weightx = 0;
        row.add(new JLabel("Car number (plate)"), gc);

        gc.gridx = 1; gc.weightx = 1;
        plateField.setColumns(16);
        row.add(plateField, gc);

        // Type
        gc.gridx = 2; gc.weightx = 0;
        row.add(new JLabel("Vehicle type"), gc);

        gc.gridx = 3; gc.weightx = 0.4;
        row.add(vehicleTypeBox, gc);

        // Buttons
        gc.gridx = 4; gc.weightx = 0;
        JButton entryBtn = primaryButton("Create Entry");
        entryBtn.addActionListener(e -> doEntry());
        row.add(entryBtn, gc);

        gc.gridx = 5;
        JButton exitBtn = dangerButton("Close Ticket");
        exitBtn.addActionListener(e -> doExit());
        row.add(exitBtn, gc);

        // Quick stats
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        stats.setOpaque(false);

        stats.add(statPill("Live", liveCountLabel));
        stats.add(statPill("History", historyCountLabel));

        card.add(row, BorderLayout.NORTH);
        card.add(stats, BorderLayout.SOUTH);

        return card;
    }

    private JComponent buildLiveTab() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton refresh = softButton("Refresh");
        refresh.addActionListener(e -> refreshLive());

        JButton closeSelected = softButton("Close Selected");
        closeSelected.addActionListener(e -> closeSelectedFromLive());

        top.add(refresh);
        top.add(closeSelected);

        root.add(top, BorderLayout.NORTH);
        root.add(wrapTable(liveTable), BorderLayout.CENTER);

        return root;
    }

    private JComponent buildHistoryTab() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton refresh = softButton("Refresh");
        refresh.addActionListener(e -> refreshHistory());

        top.add(refresh);

        root.add(top, BorderLayout.NORTH);
        root.add(wrapTable(historyTable), BorderLayout.CENTER);

        return root;
    }

    private JComponent buildBottomLog() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(0, 16, 14, 16));

        logArea.setRows(6);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(BorderFactory.createTitledBorder("Activity log"));
        root.add(sp, BorderLayout.CENTER);

        return root;
    }

    // ---------------- ACTIONS ----------------

    private void doEntry() {
        String plate = plateField.getText().trim();
        String type = (String) vehicleTypeBox.getSelectedItem();

        if (plate.isEmpty()) {
            toast("Plate cannot be empty.");
            return;
        }

        String json = "{\"plate\":\"" + escapeJson(plate) + "\",\"vehicleType\":\"" + escapeJson(type) + "\"}";
        String url = BASE_URL + "/api/tickets/entry";

        log("POST " + url + "  " + json);

        runAsync(() -> postJson(url, json),
                body -> {
                    toast("Entry created.");
                    log("Response:\n" + body);
                    refreshLive();
                },
                err -> {
                    toast("Entry failed.");
                    log("Error: " + err);
                });
    }

    private void doExit() {
        String plate = plateField.getText().trim();
        if (plate.isEmpty()) {
            toast("Enter plate, or select a row in Live and use 'Close Selected'.");
            return;
        }

        String json = "{\"plate\":\"" + escapeJson(plate) + "\"}";
        String url = BASE_URL + "/api/tickets/exit";

        log("POST " + url + "  " + json);

        runAsync(() -> postJson(url, json),
                body -> {
                    toast("Ticket closed.");
                    log("Response:\n" + body);
                    refreshLive();
                    refreshHistory();
                },
                err -> {
                    toast("Exit failed.");
                    log("Error: " + err);
                });
    }

    private void closeSelectedFromLive() {
        int row = liveTable.getSelectedRow();
        if (row < 0) {
            toast("Select a row in Live first.");
            return;
        }
        // plate is column 1
        String plate = String.valueOf(liveModel.getValueAt(row, 1));
        plateField.setText(plate);
        doExit();
    }

    private void refreshLive() {
        String url = BASE_URL + "/api/tickets?status=OPEN&sort=entryTime,desc";
        log("GET " + url);

        runAsync(() -> get(url),
                body -> {
                    List<Row> rows = parseTicketsJson(body);
                    fillLive(rows);
                    liveCountLabel.setText(String.valueOf(rows.size()));
                    pingServerOk();
                },
                err -> {
                    liveAreaFallback("Live refresh failed: " + err);
                    pingServerFail();
                });
    }

    private void refreshHistory() {
        String url = BASE_URL + "/api/tickets?status=CLOSED&sort=exitTime,desc";
        log("GET " + url);

        runAsync(() -> get(url),
                body -> {
                    List<Row> rows = parseTicketsJson(body);
                    fillHistory(rows);
                    historyCountLabel.setText(String.valueOf(rows.size()));
                    pingServerOk();
                },
                err -> {
                    historyAreaFallback("History refresh failed: " + err);
                    pingServerFail();
                });
    }

    private void pingServer() {
        String url = BASE_URL + "/health";
        log("GET " + url);

        runAsync(() -> get(url),
                body -> {
                    pingServerOk();
                    log("Health:\n" + body);
                    toast("Server reachable.");
                },
                err -> {
                    pingServerFail();
                    log("Health error: " + err);
                    toast("Server not reachable.");
                });
    }

    // ---------------- TABLE FILL ----------------

    private void fillLive(List<Row> rows) {
        liveModel.setRowCount(0);
        for (Row r : rows) {
            // Live columns: id, plate, type, entryTime, lot, slot, status
            liveModel.addRow(new Object[]{
                    r.id, r.plate, r.vehicleType, r.entryTime, nz(r.lot), nz(r.slot), r.status
            });
        }
    }

    private void fillHistory(List<Row> rows) {
        historyModel.setRowCount(0);
        for (Row r : rows) {
            historyModel.addRow(new Object[]{
                    r.id, r.plate, r.vehicleType, r.entryTime, nz(r.exitTime), nz(r.amount),
                    nz(r.lot), nz(r.slot), r.status
            });
        }
    }

    private String nz(String s) {
        return (s == null || s.isBlank() || s.equals("null")) ? "—" : s;
    }

    // If parsing fails, we still show something helpful
    private void liveAreaFallback(String msg) {
        toast(msg);
        log(msg);
    }

    private void historyAreaFallback(String msg) {
        toast(msg);
        log(msg);
    }

    // ---------------- Styling ----------------

    private void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(235, 235, 235));
        table.getTableHeader().setReorderingAllowed(false);

        // Status column color
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean isSelected, boolean hasFocus, int row, int col) {

                Component c = super.getTableCellRendererComponent(tbl, val, isSelected, hasFocus, row, col);

                // Slight zebra striping
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
                }

                // status cell highlight
                String header = tbl.getColumnName(col);
                if ("Status".equalsIgnoreCase(header) && val != null) {
                    String s = String.valueOf(val);
                    if (!isSelected) {
                        if ("OPEN".equalsIgnoreCase(s)) c.setForeground(new Color(0, 128, 0));
                        else if ("CLOSED".equalsIgnoreCase(s)) c.setForeground(new Color(90, 90, 90));
                        else c.setForeground(Color.DARK_GRAY);
                    }
                } else {
                    if (!isSelected) c.setForeground(Color.DARK_GRAY);
                }

                setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private JComponent wrapTable(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        return sp;
    }

    private JPanel statPill(String label, JLabel value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 225, 225)),
                new EmptyBorder(6, 10, 6, 10)
        ));
        p.setBackground(new Color(250, 250, 250));
        JLabel l = new JLabel(label + ":");
        l.setForeground(new Color(90, 90, 90));
        value.setFont(value.getFont().deriveFont(Font.BOLD, 14f));
        p.add(l);
        p.add(value);
        return p;
    }

    private JButton softButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                new EmptyBorder(8, 12, 8, 12)
        ));
        b.setBackground(new Color(245, 245, 245));
        return b;
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(40, 111, 235));
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        return b;
    }

    private JButton dangerButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(220, 60, 60));
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        return b;
    }

    private void pingServerOk() {
        serverStatusLabel.setText("Server: online");
        serverStatusLabel.setBackground(new Color(230, 255, 230));
        serverStatusLabel.setForeground(new Color(0, 120, 0));
        serverStatusLabel.setBorder(new EmptyBorder(6, 10, 6, 10));
    }

    private void pingServerFail() {
        serverStatusLabel.setText("Server: offline");
        serverStatusLabel.setBackground(new Color(255, 235, 235));
        serverStatusLabel.setForeground(new Color(170, 0, 0));
        serverStatusLabel.setBorder(new EmptyBorder(6, 10, 6, 10));
    }

    // ---------------- HTTP ----------------

    private String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 400) throw new RuntimeException("HTTP " + res.statusCode() + " -> " + res.body());
        return res.body();
    }

    private String postJson(String url, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 400) throw new RuntimeException("HTTP " + res.statusCode() + " -> " + res.body());
        return res.body();
    }

    private void runAsync(ThrowingSupplier supplier,
                          java.util.function.Consumer<String> onSuccess,
                          java.util.function.Consumer<String> onError) {
        new Thread(() -> {
            try {
                String body = supplier.get();
                SwingUtilities.invokeLater(() -> onSuccess.accept(body));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> onError.accept(ex.toString()));
            }
        }, "http-worker").start();
    }

    @FunctionalInterface
    private interface ThrowingSupplier { String get() throws Exception; }

    // ---------------- Logging / UX ----------------

    private void log(String msg) {
        logArea.append(msg + "\n\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void toast(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ---------------- Minimal JSON parsing ----------------
    // This is intentionally simple to avoid extra dependencies.
    // It expects an array of objects like: [{"id":1,"plate":"...","vehicleType":"CAR",...}, ...]
    // If your backend returns different names, tell me and I’ll adjust keys.

    private static class Row {
        String id, plate, vehicleType, entryTime, exitTime, amount, lot, slot, status;
    }

    private List<Row> parseTicketsJson(String json) {
        // If backend returns { ... } wrap or with HTTP headers, clean it.
        String s = json.trim();
        if (!s.startsWith("[")) {
            // sometimes backend may return an object; try to detect array inside
            int i = s.indexOf('[');
            int j = s.lastIndexOf(']');
            if (i >= 0 && j > i) s = s.substring(i, j + 1);
        }

        List<Row> rows = new ArrayList<>();
        if (!s.startsWith("[") || !s.endsWith("]")) return rows;

        // split objects (naive but works for flat JSON without nested objects)
        List<String> objects = splitTopLevelObjects(s);
        for (String obj : objects) {
            Row r = new Row();
            r.id = getJsonValue(obj, "id");
            r.plate = stripQuotes(getJsonValue(obj, "plate"));
            r.vehicleType = stripQuotes(getJsonValue(obj, "vehicleType"));
            r.entryTime = stripQuotes(getJsonValue(obj, "entryTime"));
            r.exitTime = stripQuotes(getJsonValue(obj, "exitTime"));
            r.amount = stripQuotes(getJsonValue(obj, "amount"));
            r.lot = stripQuotes(getJsonValue(obj, "lot"));
            r.slot = stripQuotes(getJsonValue(obj, "slot"));
            r.status = stripQuotes(getJsonValue(obj, "status"));
            rows.add(r);
        }
        return rows;
    }

    private static List<String> splitTopLevelObjects(String arrayJson) {
        List<String> out = new ArrayList<>();
        String s = arrayJson.trim();
        if (s.length() < 2) return out;
        // remove [ ]
        s = s.substring(1, s.length() - 1).trim();
        if (s.isEmpty()) return out;

        int depth = 0;
        boolean inQuotes = false;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) inQuotes = !inQuotes;
            if (inQuotes) continue;

            if (c == '{') depth++;
            if (c == '}') depth--;

            if (depth == 0 && c == '}' && i + 1 < s.length()) {
                // object ends; next char might be comma
                out.add(s.substring(start, i + 1).trim());
                // move start to next object (skip comma + spaces)
                int k = i + 1;
                while (k < s.length() && (s.charAt(k) == ',' || Character.isWhitespace(s.charAt(k)))) k++;
                start = k;
            }
        }
        // last object
        if (start < s.length()) out.add(s.substring(start).trim());
        return out;
    }

    private static String getJsonValue(String objJson, String key) {
        // naive: find "key": then read until comma or end
        String k = "\"" + key + "\"";
        int idx = objJson.indexOf(k);
        if (idx < 0) return null;
        int colon = objJson.indexOf(':', idx + k.length());
        if (colon < 0) return null;

        int i = colon + 1;
        while (i < objJson.length() && Character.isWhitespace(objJson.charAt(i))) i++;

        // value can be string, number, null
        if (i >= objJson.length()) return null;

        if (objJson.charAt(i) == '"') {
            int j = i + 1;
            while (j < objJson.length()) {
                if (objJson.charAt(j) == '"' && objJson.charAt(j - 1) != '\\') break;
                j++;
            }
            if (j < objJson.length()) return objJson.substring(i, j + 1);
            return objJson.substring(i);
        } else {
            int j = i;
            while (j < objJson.length() && objJson.charAt(j) != ',' && objJson.charAt(j) != '}') j++;
            return objJson.substring(i, j).trim();
        }
    }

    private static String stripQuotes(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ParkingSwingUI().setVisible(true));
    }
}