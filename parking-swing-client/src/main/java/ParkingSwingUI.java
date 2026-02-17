import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;


public class ParkingSwingUI extends JFrame {

    // === change if needed ===
    private static final String BASE_URL = "http://localhost:8080/api/tickets";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // Tables
    private final DefaultTableModel liveModel = tableModel();
    private final DefaultTableModel historyModel = tableModel();

    // Live controls
    private final JComboBox<String> liveSort = new JComboBox<>(new String[]{
            "entryTime desc", "entryTime asc", "plate asc", "plate desc"
    });

    // History controls
    private final JTextField searchPlate = new JTextField(18);
    private final JComboBox<String> filterStatus = new JComboBox<>(new String[]{
            "CLOSED", "OPEN", "ALL"
    });
    private final JComboBox<String> filterVehicle = new JComboBox<>(new String[]{
            "ALL", "CAR", "BIKE", "TRUCK", "OTHER"
    });
    private final JComboBox<String> historySort = new JComboBox<>(new String[]{
            "exitTime desc", "exitTime asc", "entryTime desc", "entryTime asc", "plate asc", "plate desc"
    });

    private final JLabel statusBar = new JLabel("Ready");

    public ParkingSwingUI() {
        super("Parking System");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 650);
        setLocationRelativeTo(null);

        setLookAndFeel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Live", buildLiveTab());
        tabs.addTab("History", buildHistoryTab());

        JPanel root = new JPanel(new BorderLayout());
        root.add(tabs, BorderLayout.CENTER);

        statusBar.setBorder(new EmptyBorder(8, 12, 8, 12));
        root.add(statusBar, BorderLayout.SOUTH);

        setContentPane(root);

        // Initial load
        loadLive();
        loadHistory();
    }

    private JPanel buildLiveTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Live (OPEN tickets)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadLive());

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.add(new JLabel("Sort:"));
        controls.add(liveSort);
        controls.add(refresh);
        top.add(controls, BorderLayout.EAST);

        liveSort.addActionListener(e -> loadLive());

        JTable table = new JTable(liveModel);
        styleTable(table);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("History");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JButton apply = new JButton("Apply");
        JButton clear = new JButton("Clear");
        JButton refresh = new JButton("Refresh");

        apply.addActionListener(e -> loadHistory());
        refresh.addActionListener(e -> loadHistory());
        clear.addActionListener(e -> {
            searchPlate.setText("");
            filterStatus.setSelectedItem("CLOSED");
            filterVehicle.setSelectedItem("ALL");
            historySort.setSelectedItem("exitTime desc");
            loadHistory();
        });

        // Enter in search triggers apply
        searchPlate.addActionListener(e -> loadHistory());

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.add(new JLabel("Search Plate:"));
        controls.add(searchPlate);

        controls.add(new JLabel("Status:"));
        controls.add(filterStatus);

        controls.add(new JLabel("Type:"));
        controls.add(filterVehicle);

        controls.add(new JLabel("Sort:"));
        controls.add(historySort);

        controls.add(apply);
        controls.add(clear);
        controls.add(refresh);

        top.add(controls, BorderLayout.EAST);

        JTable table = new JTable(historyModel);
        styleTable(table);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ---------- Data loading (async) ----------

    private void loadLive() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("status", "OPEN");
        params.put("sort", toSortParam((String) liveSort.getSelectedItem())); // "entryTime,desc"

        fetchTickets(params, tickets -> fillTable(liveModel, tickets));
    }

    private void loadHistory() {
        Map<String, String> params = new LinkedHashMap<>();

        // Status dropdown: default CLOSED. If ALL -> skip param
        String st = (String) filterStatus.getSelectedItem();
        if (!"ALL".equals(st)) params.put("status", st);

        // Vehicle type dropdown: if ALL -> skip
        String vt = (String) filterVehicle.getSelectedItem();
        if (!"ALL".equals(vt)) params.put("vehicleType", vt);

        // Plate search: if non-empty -> plate contains
        String plate = searchPlate.getText().trim();
        if (!plate.isEmpty()) params.put("plate", plate);

        params.put("sort", toSortParam((String) historySort.getSelectedItem()));

        fetchTickets(params, tickets -> fillTable(historyModel, tickets));
    }

    private void fetchTickets(Map<String, String> params, java.util.function.Consumer<List<TicketDTO>> onSuccess) {
        String url = buildUrl(BASE_URL, params);

        setStatus("Loading: " + url);

        SwingWorker<List<TicketDTO>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TicketDTO> doInBackground() throws Exception {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 400) {
                    throw new RuntimeException("HTTP " + resp.statusCode() + " : " + resp.body());
                }

                // backend returns JSON array of tickets
                return mapper.readValue(resp.body(), new TypeReference<List<TicketDTO>>() {});
            }

            @Override
            protected void done() {
                try {
                    List<TicketDTO> tickets = get();
                    onSuccess.accept(tickets);
                    setStatus("Loaded " + tickets.size() + " tickets.");
                } catch (Exception ex) {
                    setStatus("Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(ParkingSwingUI.this,
                            ex.getMessage(), "API Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // ---------- Helpers ----------

    private static String buildUrl(String base, Map<String, String> params) {
        if (params == null || params.isEmpty()) return base;

        String q = params.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
        return base + "?" + q;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // UI dropdown uses "exitTime desc" => API wants "exitTime,desc"
    private static String toSortParam(String uiVal) {
        if (uiVal == null || uiVal.isBlank()) return "";
        String[] parts = uiVal.trim().split("\\s+");
        if (parts.length == 2) return parts[0] + "," + parts[1];
        return uiVal.trim();
    }

    private static DefaultTableModel tableModel() {
        return new DefaultTableModel(new Object[]{
                "ID", "Plate", "Type", "Status", "Entry Time", "Exit Time", "Amount"
        }, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static void fillTable(DefaultTableModel model, List<TicketDTO> tickets) {
        model.setRowCount(0);
        for (TicketDTO t : tickets) {
            model.addRow(new Object[]{
                    t.id,
                    t.plate,
                    t.vehicleType,
                    t.status,
                    t.entryTime,
                    t.exitTime,
                    t.amount
            });
        }
    }

    private static void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
    }

    private void setStatus(String msg) {
        statusBar.setText(msg);
    }

    private static void setLookAndFeel() {
        try {
            // Nimbus looks decent out of the box
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    // ---------- DTO matching your JSON fields ----------
    public static class TicketDTO {
        public Long id;
        public String plate;
        public String vehicleType;
        public String status;
        public String entryTime;  // keeping as String for simplicity (matches JSON)
        public String exitTime;
        public Double amount;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ParkingSwingUI().setVisible(true));
    }
}
