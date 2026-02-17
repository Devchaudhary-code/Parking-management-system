import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ParkingSwingUI extends JFrame {

    private static final String BASE_URL = "http://localhost:8080/api/tickets";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final DefaultTableModel liveModel = createTableModel();
    private final DefaultTableModel historyModel = createTableModel();

    private final JComboBox<SortOption> liveSort = new JComboBox<>(new SortOption[]{
            new SortOption("Entry time • newest", "entryTime,desc"),
            new SortOption("Entry time • oldest", "entryTime,asc"),
            new SortOption("Plate • A → Z", "plate,asc"),
            new SortOption("Plate • Z → A", "plate,desc")
    });

    private final JTextField plateSearch = new JTextField(18);
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"CLOSED", "OPEN", "ALL"});
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{"ALL", "CAR", "BIKE", "TRUCK", "OTHER"});
    private final JComboBox<SortOption> historySort = new JComboBox<>(new SortOption[]{
            new SortOption("Exit time • newest", "exitTime,desc"),
            new SortOption("Exit time • oldest", "exitTime,asc"),
            new SortOption("Entry time • newest", "entryTime,desc"),
            new SortOption("Entry time • oldest", "entryTime,asc"),
            new SortOption("Plate • A → Z", "plate,asc"),
            new SortOption("Plate • Z → A", "plate,desc")
    });

    private final JLabel statusBar = new JLabel("Ready");
    private final JProgressBar progress = new JProgressBar();

    public ParkingSwingUI() {
        super("Parking • Control Panel");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);

        setContentPane(buildRoot());
        refreshLive();
        refreshHistory();
    }

    private JComponent buildRoot() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        // top app bar
        root.add(buildTopBar(), BorderLayout.NORTH);

        // tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.putClientProperty("JTabbedPane.tabHeight", 40);
        tabs.putClientProperty("JTabbedPane.tabArc", 14);

        tabs.addTab("Live", buildLivePanel());
        tabs.addTab("History", buildHistoryPanel());

        root.add(tabs, BorderLayout.CENTER);

        // bottom bar
        root.add(buildBottomBar(), BorderLayout.SOUTH);

        return root;
    }

    private JComponent buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("Parking Dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel subtitle = new JLabel("Search • Sort • Filter (API-driven)");
        subtitle.setForeground(new Color(160, 160, 160));
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(title);
        left.add(subtitle);

        JButton quickRefresh = new JButton("Refresh All");
        quickRefresh.putClientProperty("JButton.buttonType", "roundRect");
        quickRefresh.addActionListener(e -> {
            refreshLive();
            refreshHistory();
        });

        top.add(left, BorderLayout.WEST);
        top.add(quickRefresh, BorderLayout.EAST);
        return top;
    }

    private JComponent buildBottomBar() {
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setBorder(new EmptyBorder(10, 0, 0, 0));

        progress.setIndeterminate(true);
        progress.setVisible(false);

        statusBar.setBorder(new EmptyBorder(0, 6, 0, 0));
        statusBar.setForeground(new Color(170, 170, 170));

        bottom.add(progress, BorderLayout.WEST);
        bottom.add(statusBar, BorderLayout.CENTER);
        return bottom;
    }

    // ---------- Live tab ----------

    private JComponent buildLivePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel header = cardHeader("Live Tickets", "Currently parked vehicles (status OPEN)");

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.putClientProperty("JButton.buttonType", "roundRect");
        refreshBtn.addActionListener(e -> refreshLive());

        liveSort.addActionListener(e -> refreshLive());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);
        controls.add(labelMuted("Sort"));
        controls.add(styleCombo(liveSort));
        controls.add(refreshBtn);

        header.add(controls, BorderLayout.EAST);

        JTable table = new JTable(liveModel);
        styleTable(table);

        panel.add(header, BorderLayout.NORTH);
        panel.add(wrapTable(table), BorderLayout.CENTER);
        return panel;
    }

    private void refreshLive() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("status", "OPEN");
        params.put("sort", selectedSortValue(liveSort));
        fetchTickets(params, tickets -> fillModel(liveModel, tickets));
    }

    // ---------- History tab ----------

    private JComponent buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel header = cardHeader("History", "Past tickets (usually CLOSED) with search and filters");

        // Placeholder text (FlatLaf supports it)
        plateSearch.putClientProperty("JTextField.placeholderText", "Search plate (e.g., MH12)");
        plateSearch.putClientProperty("JTextField.leadingIcon", UIManager.getIcon("Tree.leafIcon"));

        JButton applyBtn = new JButton("Apply");
        JButton clearBtn = new JButton("Clear");
        JButton refreshBtn = new JButton("Refresh");

        applyBtn.putClientProperty("JButton.buttonType", "roundRect");
        clearBtn.putClientProperty("JButton.buttonType", "roundRect");
        refreshBtn.putClientProperty("JButton.buttonType", "roundRect");

        applyBtn.addActionListener(e -> refreshHistory());
        refreshBtn.addActionListener(e -> refreshHistory());
        clearBtn.addActionListener(e -> {
            plateSearch.setText("");
            statusFilter.setSelectedItem("CLOSED");
            typeFilter.setSelectedItem("ALL");
            historySort.setSelectedIndex(0);
            refreshHistory();
        });

        plateSearch.addActionListener(e -> refreshHistory());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);

        controls.add(labelMuted("Plate"));
        controls.add(styleText(plateSearch));

        controls.add(labelMuted("Status"));
        controls.add(styleCombo(statusFilter));

        controls.add(labelMuted("Type"));
        controls.add(styleCombo(typeFilter));

        controls.add(labelMuted("Sort"));
        controls.add(styleCombo(historySort));

        controls.add(applyBtn);
        controls.add(clearBtn);
        controls.add(refreshBtn);

        header.add(controls, BorderLayout.EAST);

        JTable table = new JTable(historyModel);
        styleTable(table);

        panel.add(header, BorderLayout.NORTH);
        panel.add(wrapTable(table), BorderLayout.CENTER);
        return panel;
    }

    private void refreshHistory() {
        Map<String, String> params = new LinkedHashMap<>();

        String status = Objects.toString(statusFilter.getSelectedItem(), "ALL");
        if (!"ALL".equals(status)) params.put("status", status);

        String type = Objects.toString(typeFilter.getSelectedItem(), "ALL");
        if (!"ALL".equals(type)) params.put("vehicleType", type);

        String plate = plateSearch.getText().trim();
        if (!plate.isEmpty()) params.put("plate", plate);

        params.put("sort", selectedSortValue(historySort));

        fetchTickets(params, tickets -> fillModel(historyModel, tickets));
    }

    // ---------- HTTP ----------

    private void fetchTickets(Map<String, String> params, java.util.function.Consumer<List<TicketDTO>> onOk) {
        final String url = buildUrl(BASE_URL, params);
        setBusy(true, "Loading…");

        new SwingWorker<List<TicketDTO>, Void>() {
            @Override
            protected List<TicketDTO> doInBackground() throws Exception {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 400) {
                    throw new RuntimeException("HTTP " + resp.statusCode() + "  " + resp.body());
                }
                return mapper.readValue(resp.body(), new TypeReference<List<TicketDTO>>() {});
            }

            @Override
            protected void done() {
                try {
                    List<TicketDTO> tickets = get();
                    onOk.accept(tickets);
                    setBusy(false, "Loaded " + tickets.size() + " ticket(s).");
                } catch (Exception ex) {
                    setBusy(false, "Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(ParkingSwingUI.this,
                            ex.getMessage(), "API Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ---------- UI helpers ----------

    private static JPanel cardHeader(String title, String subtitle) {
        JPanel card = new JPanel(new BorderLayout());
        card.putClientProperty("JComponent.arc", 18);
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 18f));

        JLabel sub = new JLabel(subtitle);
        sub.setForeground(new Color(160, 160, 160));
        sub.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(t);
        left.add(sub);

        card.add(left, BorderLayout.WEST);
        return card;
    }

    private static JLabel labelMuted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(170, 170, 170));
        return l;
    }

    private static JComponent wrapTable(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setOpaque(false);
        return sp;
    }

    private static JComboBox<?> styleCombo(JComboBox<?> combo) {
        combo.putClientProperty("JComponent.arc", 14);
        combo.putClientProperty("JComponent.minimumWidth", 160);
        return combo;
    }

    private static JTextField styleText(JTextField field) {
        field.putClientProperty("JComponent.arc", 14);
        field.putClientProperty("JTextField.showClearButton", true);
        return field;
    }

    private static void styleTable(JTable table) {
        table.setRowHeight(32);
        table.setFillsViewportHeight(true);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        // center some columns for that “dashboard” look
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        table.getColumnModel().getColumn(0).setCellRenderer(center); // ID
        table.getColumnModel().getColumn(2).setCellRenderer(center); // Type
        table.getColumnModel().getColumn(3).setCellRenderer(center); // Status
    }

    private void setBusy(boolean busy, String msg) {
        progress.setVisible(busy);
        statusBar.setText(msg);
    }

    // ---------- data helpers ----------

    private static DefaultTableModel createTableModel() {
        return new DefaultTableModel(new Object[]{
                "ID", "Plate", "Type", "Status", "Entry Time", "Exit Time", "Amount"
        }, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static void fillModel(DefaultTableModel model, List<TicketDTO> tickets) {
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

    private static String buildUrl(String base, Map<String, String> params) {
        if (params == null || params.isEmpty()) return base;

        StringBuilder sb = new StringBuilder(base).append("?");
        boolean first = true;
        for (var e : params.entrySet()) {
            if (!first) sb.append("&");
            first = false;
            sb.append(enc(e.getKey())).append("=").append(enc(e.getValue()));
        }
        return sb.toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String selectedSortValue(JComboBox<SortOption> combo) {
        SortOption opt = (SortOption) combo.getSelectedItem();
        return opt == null ? "" : opt.apiValue;
    }

    // ---------- tiny helper types ----------

    private static class SortOption {
        final String label;
        final String apiValue;

        SortOption(String label, String apiValue) {
            this.label = label;
            this.apiValue = apiValue;
        }

        @Override public String toString() { return label; }
    }

    public static class TicketDTO {
        public Long id;
        public String plate;
        public String vehicleType;
        public String status;
        public String entryTime;
        public String exitTime;
        public Double amount;
    }

    // ---------- main ----------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup(); // modern dark theme

            // A few global tweaks (optional but nice)
            UIManager.put("Component.arc", 14);
            UIManager.put("Button.arc", 14);
            UIManager.put("TextComponent.arc", 14);
            UIManager.put("ScrollBar.width", 12);

            new ParkingSwingUI().setVisible(true);
        });
    }
}
