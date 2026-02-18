import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginUI extends JFrame {

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel statusLabel = new JLabel(" ");

    public LoginUI() {
        super("Parking • Login");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(460, 340);

        setLocationRelativeTo(null);
        setResizable(false);


        usernameField.setPreferredSize(new Dimension(320, 36));
        passwordField.setPreferredSize(new Dimension(320, 36));


        usernameField.putClientProperty("JTextField.placeholderText", "e.g., admin");
        passwordField.putClientProperty("JTextField.placeholderText", "Your password");

        setContentPane(buildUI());
    }

    private JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(18, 18, 18, 18));
        root.setBackground(Color.WHITE);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildForm(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        return root;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("Login");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));

        JLabel subtitle = new JLabel("Enter your credentials to continue");
        subtitle.setForeground(new Color(90, 90, 90));

        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(14));

        return header;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(6, 0, 6, 0);

        // Username
        gbc.gridy = 0;
        form.add(labelMuted("Username"), gbc);

        gbc.gridy = 1;
        form.add(usernameField, gbc);

        // Password
        gbc.gridy = 2;
        form.add(labelMuted("Password"), gbc);

        gbc.gridy = 3;
        form.add(passwordField, gbc);

        return form;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(14, 0, 0, 0));

        statusLabel.setForeground(new Color(180, 0, 0));
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton exitBtn = pillButton("Exit");
        JButton loginBtn = pillButton("Login");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.add(exitBtn);
        buttons.add(loginBtn);


        loginBtn.addActionListener(e -> attemptLogin());
        exitBtn.addActionListener(e -> System.exit(0));
        usernameField.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());

        footer.add(statusLabel);
        footer.add(Box.createVerticalStrut(10));
        footer.add(buttons);

        return footer;
    }

    private void attemptLogin() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());

        if (u.isEmpty() || p.isEmpty()) {
            setStatus("Please enter username and password.");
            return;
        }


        if (!(u.equals("admin") && p.equals("admin123"))) {
            setStatus("Invalid username or password.");
            passwordField.setText("");
            passwordField.requestFocusInWindow();
            return;
        }


        SwingUtilities.invokeLater(() -> {
            dispose();
            new ParkingHistoryUI().setVisible(true);
        });
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private static JLabel labelMuted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(60, 60, 60));
        return l;
    }

    private static JButton pillButton(String text) {
        JButton b = new JButton(text);
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setFocusable(false);
        b.setPreferredSize(new Dimension(110, 36));
        return b;
    }
}
