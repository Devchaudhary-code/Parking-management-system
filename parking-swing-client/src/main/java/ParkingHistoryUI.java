import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.FlatLightLaf;

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

public class ParkingHistoryUI extends JFrame {

    private static final String API_BASE = "http://localhost:8080/api/tickets";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Plate", "Type", "Status", "Entry Time", "Exit Time", "Amount"},
            0
    ) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    // Controls
    private final JTextField plateField = new JTextField(18);
    private final JComboBox<String> statusBox = new JComboBox<>(new String[]{"CLOSED", "OPEN", "ALL"});
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"ALL", "CAR", "BIKE", "TRUCK", "OTHER"});
    private final JComboBox<SortOption> sortBox = new JComboBox<>(new SortOption[]{
            new SortOption("Exit time (newest)", "exitTime,desc"),
            new SortOption("Exit time (oldest)", "exitTime,asc"),
            new SortOption("Entry time (newest)", "entryTime,desc"),
            new SortOption("Entry time (oldest)", "entryTime,asc"),
            new SortOption("Plate (A → Z)", "plate,asc"),
            new SortOption("Plate (Z → A)", "plate,desc")
    });

    private final JLabel statusText = new JLabel("Ready");
    private final JProgressBar progress = new JProgressBar();

    public ParkingHistoryUI() {
        super("Parking • History");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);

        setContentPane(buildRoot());
        wireActions();


        refresh();
    }

    private JComponent buildRoot() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildTable(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        return root;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.setBorder(new EmptyBorder(0, 0, 12, 0));

        // Title block
        JLabel title = new JLabel("History");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel subtitle = new JLabel("Search by plate • Filter by status/type • Sort results");
        subtitle.setForeground(new Color(90, 90, 90));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(subtitle);

        // Filter bar
        plateField.putClientProperty("JTextField.placeholderText", "Plate contains… (e.g., MH12)");
        plateField.putClientProperty("JTextField.showClearButton", true);

        JButton apply = pillButton("Apply");
        JButton clear = pillButton("Clear");
        JButton refresh = pillButton("Refresh");

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filters.setOpaque(false);

        filters.add(labelMuted("Plate"));
        filters.add(plateField);

        filters.add(labelMuted("Status"));
        filters.add(statusBox);

        filters.add(labelMuted("Type"));
        filters.add(typeBox);

        filters.add(labelMuted("Sort"));
        filters.add(sortBox);

        filters.add(apply);
        filters.add(clear);
        filters.add(refresh);


        apply.addActionListener(e -> refresh());
        refresh.addActionListener(e -> refresh());
        clear.addActionListener(e -> {
            plateField.setText("");
            statusBox.setSelectedItem("CLOSED");
            typeBox.setSelectedItem("ALL");
            sortBox.setSelectedIndex(0);
            refresh();
        });

        header.add(titleBlock, BorderLayout.WEST);
        header.add(filters, BorderLayout.EAST);


        JPanel divider = new JPanel();
        divider.setPreferredSize(new Dimension(1, 1));
        divider.setBackground(new Color(230, 230, 230));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(header, BorderLayout.CENTER);
        wrapper.add(divider, BorderLayout.SOUTH);

        return wrapper;
    }

    private JComponent buildTable() {
        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.setFillsViewportHeight(true);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);
        table.getColumnModel().getColumn(3).setCellRenderer(center);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new EmptyBorder(12, 0, 0, 0));
        return sp;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setBorder(new EmptyBorder(12, 0, 0, 0));

        progress.setIndeterminate(true);
        progress.setVisible(false);

        statusText.setForeground(new Color(110, 110, 110));

        footer.add(progress, BorderLayout.WEST);
        footer.add(statusText, BorderLayout.CENTER);

        return footer;
    }

    private void wireActions() {
        plateField.addActionListener(e -> refresh());
        statusBox.addActionListener(e -> refresh());
        typeBox.addActionListener(e -> refresh());
        sortBox.addActionListener(e -> refresh());
    }

    private void refresh() {
        Map<String, String> params = new LinkedHashMap<>();

        String plate = plateField.getText().trim();
        if (!plate.isEmpty()) params.put("plate", plate);

        String status = Objects.toString(statusBox.getSelectedItem(), "ALL");
        if (!"ALL".equals(status)) params.put("status", status);

        String type = Objects.toString(typeBox.getSelectedItem(), "ALL");
        if (!"ALL".equals(type)) params.put("vehicleType", type);

        SortOption sort = (SortOption) sortBox.getSelectedItem();
        if (sort != null && sort.apiValue != null && !sort.apiValue.isBlank()) {
            params.put("sort", sort.apiValue);
        }

        fetchTickets(params);
    }

    private void fetchTickets(Map<String, String> params) {
        String url = buildUrl(API_BASE, params);
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
                    throw new RuntimeException("HTTP " + resp.statusCode() + "\n" + resp.body());
                }
                return mapper.readValue(resp.body(), new TypeReference<List<TicketDTO>>() {});
            }

            @Override
            protected void done() {
                try {
                    List<TicketDTO> list = get();
                    fillTable(list);
                    setBusy(false, "Loaded " + list.size() + " ticket(s).");
                } catch (Exception ex) {
                    setBusy(false, "Error");
                    JOptionPane.showMessageDialog(ParkingHistoryUI.this,
                            ex.getMessage(), "API Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void fillTable(List<TicketDTO> list) {
        model.setRowCount(0);
        for (TicketDTO t : list) {
            model.addRow(new Object[]{
                    t.id, t.plate, t.vehicleType, t.status, t.entryTime, t.exitTime, t.amount
            });
        }
    }

    private void setBusy(boolean busy, String msg) {
        progress.setVisible(busy);
        statusText.setText(msg);
    }

    private static JButton pillButton(String text) {
        JButton b = new JButton(text);
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private static JLabel labelMuted(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(new Color(110, 110, 110));
        return l;
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

    private static class SortOption {
        final String label;
        final String apiValue;
        SortOption(String label, String apiValue) { this.label = label; this.apiValue = apiValue; }
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            UIManager.put("Component.arc", 14);
            UIManager.put("Button.arc", 14);
            UIManager.put("TextComponent.arc", 14);
            UIManager.put("ScrollBar.width", 12);

            new LoginUI().setVisible(true);
        });
    }


}
