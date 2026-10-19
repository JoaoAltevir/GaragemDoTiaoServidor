package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import entities.SessionUser;
import entities.User;
import service.ServerService;
import service.SessionService;
import service.UserService;

public class HomeWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private static final Color BACKGROUND = new Color(245, 247, 250);
	private static final Color TEXT = new Color(35, 45, 58);
	private static final Color MUTED_TEXT = new Color(100, 112, 128);
	private static final Color ACCENT = new Color(36, 95, 156);

	private JTable table_users;
	private ServerService serverService;
	private SessionService sessionService;
	private UserService userService;
	private MainWindow mainWindow;

	public HomeWindow(int port, MainWindow main) {
		this.mainWindow = main;
		this.sessionService = new SessionService();
		this.serverService = new ServerService(port, this);
		this.userService = new UserService(this);

		initComponents(port);
		this.serverService.iniciarServidor();
		this.userService.getAll();
	}

	public void refreshUserTable(List<User> users) {
		SwingUtilities.invokeLater(() -> {
			DefaultTableModel model = (DefaultTableModel) table_users.getModel();
			model.setRowCount(0);

			for (User user : users) {
				SessionUser sessionUser = sessionService.isLogged(user.getUsername());
				String ipAddress = sessionUser == null ? "-" : sessionUser.getIpAddress();
				String status = sessionUser == null ? "Offline" : "Online";

				model.addRow(new Object[] {
					user.getName(),
					user.getUsername(),
					ipAddress == null || ipAddress.isBlank() ? "-" : ipAddress,
					user.getRole(),
					status
				});
			}
		});
	}

	public void pararServidor() {
		System.out.println("Parando servidor... Obrigado por acessar a garagem do Tiao!");
		this.mainWindow.setVisible(true);
		this.serverService.fecharServidor();
		this.dispose();
	}

	private void initComponents(int port) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Servidor - Garagem do Tiao");
		setSize(900, 560);
		setMinimumSize(new java.awt.Dimension(700, 440));
		setLocationRelativeTo(null);

		JPanel contentPane = new JPanel(new BorderLayout(0, 20));
		contentPane.setBackground(BACKGROUND);
		contentPane.setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
		setContentPane(contentPane);

		JPanel header = new JPanel(new BorderLayout(0, 5));
		header.setOpaque(false);
		JLabel title = new JLabel("Painel do servidor");
		title.setFont(new Font("SansSerif", Font.BOLD, 24));
		title.setForeground(TEXT);
		JLabel subtitle = new JLabel("Servidor ativo na porta " + port);
		subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
		subtitle.setForeground(MUTED_TEXT);
		header.add(title, BorderLayout.NORTH);
		header.add(subtitle, BorderLayout.CENTER);
		contentPane.add(header, BorderLayout.NORTH);

		JPanel tablePanel = new JPanel(new BorderLayout(0, 12));
		tablePanel.setBackground(Color.WHITE);
		tablePanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(222, 228, 235)),
			BorderFactory.createEmptyBorder(16, 16, 16, 16)
		));

		JLabel tableTitle = new JLabel("Usuários");
		tableTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
		tableTitle.setForeground(TEXT);
		tablePanel.add(tableTitle, BorderLayout.NORTH);

		String[] columns = {"Nome", "Usuário", "IP", "Papel", "Status"};
		DefaultTableModel model = new DefaultTableModel(null, columns) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		table_users = new JTable(model);
		table_users.setFont(new Font("SansSerif", Font.PLAIN, 13));
		table_users.setForeground(TEXT);
		table_users.setRowHeight(32);
		table_users.setShowVerticalLines(false);
		table_users.setShowHorizontalLines(true);
		table_users.setGridColor(new Color(232, 236, 241));
		table_users.setIntercellSpacing(new java.awt.Dimension(0, 1));
		table_users.setSelectionBackground(new Color(220, 235, 250));
		table_users.setSelectionForeground(TEXT);
		table_users.setFillsViewportHeight(true);
		table_users.setAutoCreateRowSorter(true);

		DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer();
		textRenderer.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		for (int column = 0; column < columns.length - 1; column++) {
			table_users.getColumnModel().getColumn(column).setCellRenderer(textRenderer);
		}
		table_users.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
		configureColumnWidths(table_users.getColumnModel());

		JScrollPane scrollPane = new JScrollPane(table_users);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(222, 228, 235)));
		scrollPane.getViewport().setBackground(Color.WHITE);
		tablePanel.add(scrollPane, BorderLayout.CENTER);
		contentPane.add(tablePanel, BorderLayout.CENTER);

		JPanel footer = new JPanel(new BorderLayout());
		footer.setOpaque(false);

		JPanel managementButtons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
		managementButtons.setOpaque(false);
		JButton btn_update = createButton("Atualizar", false);
		JButton btn_delete = createButton("Apagar", false);
		JButton btn_add = createButton("Adicionar", true);
		managementButtons.add(btn_update);
		managementButtons.add(btn_delete);
		managementButtons.add(btn_add);

		JButton btn_exit = createButton("Sair", false);
		btn_exit.addActionListener(e -> pararServidor());
		footer.add(btn_exit, BorderLayout.WEST);
		footer.add(managementButtons, BorderLayout.EAST);
		contentPane.add(footer, BorderLayout.SOUTH);
	}

	private void configureColumnWidths(TableColumnModel columns) {
		columns.getColumn(0).setPreferredWidth(190);
		columns.getColumn(1).setPreferredWidth(145);
		columns.getColumn(2).setPreferredWidth(150);
		columns.getColumn(3).setPreferredWidth(100);
		columns.getColumn(4).setPreferredWidth(105);
	}

	private JButton createButton(String text, boolean primary) {
		JButton button = new JButton(text);
		button.setFont(new Font("SansSerif", Font.BOLD, 13));
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(primary ? ACCENT : new Color(205, 213, 222)),
			BorderFactory.createEmptyBorder(8, 14, 8, 14)
		));
		if (primary) {
			button.setBackground(ACCENT);
			button.setForeground(Color.WHITE);
		} else {
			button.setBackground(Color.WHITE);
			button.setForeground(TEXT);
		}
		return button;
	}
}

class StatusRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;
	private static final Color ONLINE = new Color(31, 143, 86);
	private static final Color OFFLINE = new Color(190, 63, 63);

	@Override
	public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		JLabel label = (JLabel) super.getTableCellRendererComponent(
			table, value, isSelected, hasFocus, row, column);

		boolean online = "Online".equalsIgnoreCase(String.valueOf(value));
		Color statusColor = online ? ONLINE : OFFLINE;
		label.setIcon(new BolinhaIcon(statusColor));
		label.setForeground(isSelected ? table.getSelectionForeground() : statusColor);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
		return label;
	}
}

class BolinhaIcon implements Icon {
	private final Color color;

	public BolinhaIcon(Color color) {
		this.color = color;
	}

	@Override
	public void paintIcon(Component component, Graphics graphics, int x, int y) {
		Graphics2D g2 = (Graphics2D) graphics.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(color);
		g2.fillOval(x, y + 2, 10, 10);
		g2.dispose();
	}

	@Override
	public int getIconWidth() {
		return 15;
	}

	@Override
	public int getIconHeight() {
		return 15;
	}
}
