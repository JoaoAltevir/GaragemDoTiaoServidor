package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import com.google.gson.JsonObject;

import entities.Session;
import entities.SessionUser;
import entities.User;
import entities.Vaga;
import entities.Vehicle;
import service.ServerService;
import service.SessionService;
import service.UserService;
import service.VagaService;
import service.VehicleService;

public class HomeWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private static final Color BACKGROUND = new Color(245, 247, 250);
	private static final Color TEXT = new Color(35, 45, 58);
	private static final Color MUTED_TEXT = new Color(100, 112, 128);
	private static final Color ACCENT = new Color(36, 95, 156);

	private JTabbedPane tabbedPane;
	private JTable table_users;
	private JTable table_vagas;
	private JTable table_vehicles;
	private JLabel lbl_status;
	private JButton btn_add;
	private JButton btn_update;
	private JButton btn_delete;
	private JButton btn_find;
	private JButton btn_reload;

	private ServerService serverService;
	private SessionService sessionService;
	private UserService userService;
	private VagaService vagaService;
	private VehicleService vehicleService;
	private MainWindow mainWindow;

	public HomeWindow(int port, MainWindow main) {
		this.mainWindow = main;
		this.sessionService = new SessionService();
		this.serverService = new ServerService(port, this);
		this.userService = new UserService(this);
		this.vagaService = new VagaService();
		this.vehicleService = new VehicleService();

		initComponents(port);
		this.serverService.iniciarServidor();
		recarregarDadosAssincrono();
	}

	public void refreshUserTable(List<User> users) {
		SwingUtilities.invokeLater(() -> {
			if (table_users == null) return;
			DefaultTableModel model = (DefaultTableModel) table_users.getModel();
			model.setRowCount(0);

			if (users == null) return;
			for (User user : users) {
				SessionUser sessionUser = sessionService.isLogged(user.getUsername());
				String ipAddress = sessionUser == null ? "-" : sessionUser.getIpAddress();
				String status = sessionUser == null ? "Offline" : "Online";

				model.addRow(new Object[] {
					user.getName(),
					user.getUsername(),
					ipAddress == null || ipAddress.isBlank() ? "-" : ipAddress,
					user.getRole() != null ? user.getRole().toLowerCase() : "user",
					status
				});
			}
		});
	}

	public void refreshVagaTable(List<Vaga> vagas) {
		SwingUtilities.invokeLater(() -> {
			if (table_vagas == null) return;
			DefaultTableModel model = (DefaultTableModel) table_vagas.getModel();
			model.setRowCount(0);

			if (vagas == null) return;
			for (Vaga vaga : vagas) {
				String plate = vaga.getLicensePlate();
				model.addRow(new Object[] {
					vaga.getSpotName(),
					vaga.getFloor(),
					vaga.getType(),
					vaga.getStatus(),
					plate == null || plate.isBlank() ? "-" : plate
				});
			}
		});
	}

	public void refreshVehicleTable(List<Vehicle> vehicles) {
		SwingUtilities.invokeLater(() -> {
			if (table_vehicles == null) return;
			DefaultTableModel model = (DefaultTableModel) table_vehicles.getModel();
			model.setRowCount(0);

			if (vehicles == null) return;
			for (Vehicle v : vehicles) {
				model.addRow(new Object[] {
					v.getLicensePlate(),
					v.getBrand(),
					v.getModel(),
					v.getType(),
					v.getUsername() != null ? v.getUsername() : "-"
				});
			}
		});
	}

	public void refreshAllTables() {
		refreshUserTable(sessionService.getAllUsers());
		refreshVagaTable(sessionService.getAllVagas());
		refreshVehicleTable(sessionService.getAllVehicles());
	}

	public void pararServidor() {
		System.out.println("Parando servidor... Obrigado por acessar a garagem do Tiao!");
		this.mainWindow.setVisible(true);
		this.serverService.fecharServidor();
		this.dispose();
	}

	private void setLoading(boolean loading, String statusText) {
		SwingUtilities.invokeLater(() -> {
			setCursor(loading ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
			if (btn_add != null) btn_add.setEnabled(!loading);
			if (btn_update != null) btn_update.setEnabled(!loading);
			if (btn_delete != null) btn_delete.setEnabled(!loading);
			if (btn_find != null) btn_find.setEnabled(!loading);
			if (btn_reload != null) btn_reload.setEnabled(!loading);
			if (lbl_status != null && statusText != null) {
				lbl_status.setText(statusText);
			}
		});
	}

	private void recarregarDadosAssincrono() {
		setLoading(true, "Carregando dados do servidor...");
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() {
				userService.getAll();
				return null;
			}

			@Override
			protected void done() {
				refreshAllTables();
				setLoading(false, "Dados sincronizados");
			}
		}.execute();
	}

	private void executarOperacao(String descricao, Supplier<JsonObject> supplier) {
		setLoading(true, descricao + "...");
		new SwingWorker<JsonObject, Void>() {
			@Override
			protected JsonObject doInBackground() {
				return supplier.get();
			}

			@Override
			protected void done() {
				try {
					JsonObject res = get();
					userService.getAll();
					refreshAllTables();

					int statusCode = res != null && res.has("statusCode") ? res.get("statusCode").getAsInt() : 500;
					String message = res != null && res.has("message") && !res.get("message").isJsonNull()
							? res.get("message").getAsString()
							: "Operação concluída.";

					setLoading(false, "[" + statusCode + "] " + message);

					if (statusCode == 200 || statusCode == 201) {
						JOptionPane.showMessageDialog(HomeWindow.this, message, "Sucesso (" + statusCode + ")", JOptionPane.INFORMATION_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(HomeWindow.this, message, "Falha (" + statusCode + ")", JOptionPane.WARNING_MESSAGE);
					}
				} catch (Exception ex) {
					setLoading(false, "Erro ao executar operação");
					JOptionPane.showMessageDialog(HomeWindow.this, "Erro inesperado: " + ex.getMessage(), "Erro (500)", JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	private void initComponents(int port) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Servidor - Garagem do Tiao");
		setSize(960, 600);
		setMinimumSize(new java.awt.Dimension(760, 480));
		setLocationRelativeTo(null);

		JPanel contentPane = new JPanel(new BorderLayout(0, 18));
		contentPane.setBackground(BACKGROUND);
		contentPane.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));
		setContentPane(contentPane);

		JPanel header = new JPanel(new BorderLayout(0, 5));
		header.setOpaque(false);

		JPanel headerLeft = new JPanel(new BorderLayout(0, 4));
		headerLeft.setOpaque(false);
		JLabel title = new JLabel("Painel do servidor");
		title.setFont(new Font("SansSerif", Font.BOLD, 24));
		title.setForeground(TEXT);
		JLabel subtitle = new JLabel("Servidor ativo na porta " + port);
		subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
		subtitle.setForeground(MUTED_TEXT);
		headerLeft.add(title, BorderLayout.NORTH);
		headerLeft.add(subtitle, BorderLayout.CENTER);

		JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
		headerRight.setOpaque(false);
		lbl_status = new JLabel("Pronto");
		lbl_status.setFont(new Font("SansSerif", Font.PLAIN, 13));
		lbl_status.setForeground(MUTED_TEXT);
		btn_reload = createButton("Recarregar", false);
		btn_reload.addActionListener(e -> recarregarDadosAssincrono());
		headerRight.add(lbl_status);
		headerRight.add(btn_reload);

		header.add(headerLeft, BorderLayout.WEST);
		header.add(headerRight, BorderLayout.EAST);
		contentPane.add(header, BorderLayout.NORTH);

		tabbedPane = new JTabbedPane();
		tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));
		tabbedPane.setBackground(Color.WHITE);
		tabbedPane.setForeground(TEXT);

		tabbedPane.addTab("Usuários", createUsersTab());
		tabbedPane.addTab("Vagas", createVagasTab());
		tabbedPane.addTab("Veículos", createVehiclesTab());

		contentPane.add(tabbedPane, BorderLayout.CENTER);

		JPanel footer = new JPanel(new BorderLayout());
		footer.setOpaque(false);

		JPanel managementButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		managementButtons.setOpaque(false);
		btn_find = createButton("Localizar Placa", false);
		btn_update = createButton("Atualizar", false);
		btn_delete = createButton("Apagar", false);
		btn_add = createButton("Adicionar", true);

		btn_find.addActionListener(e -> acaoLocalizarVeiculo());
		btn_add.addActionListener(e -> acaoAdicionar());
		btn_update.addActionListener(e -> acaoAtualizar());
		btn_delete.addActionListener(e -> acaoApagar());

		managementButtons.add(btn_find);
		managementButtons.add(btn_update);
		managementButtons.add(btn_delete);
		managementButtons.add(btn_add);

		JButton btn_exit = createButton("Sair", false);
		btn_exit.addActionListener(e -> pararServidor());
		footer.add(btn_exit, BorderLayout.WEST);
		footer.add(managementButtons, BorderLayout.EAST);
		contentPane.add(footer, BorderLayout.SOUTH);
	}

	private JPanel createUsersTab() {
		JPanel tablePanel = new JPanel(new BorderLayout(0, 12));
		tablePanel.setBackground(Color.WHITE);
		tablePanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(222, 228, 235)),
			BorderFactory.createEmptyBorder(16, 16, 16, 16)
		));

		JLabel tableTitle = new JLabel("Usuários Cadastrados e Sessões");
		tableTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
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

		table_users = createStyledTable(model);

		DefaultTableCellRenderer textRenderer = createTextRenderer();
		for (int column = 0; column < columns.length - 1; column++) {
			table_users.getColumnModel().getColumn(column).setCellRenderer(textRenderer);
		}
		table_users.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
		configureUserColumnWidths(table_users.getColumnModel());

		JScrollPane scrollPane = new JScrollPane(table_users);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(222, 228, 235)));
		scrollPane.getViewport().setBackground(Color.WHITE);
		tablePanel.add(scrollPane, BorderLayout.CENTER);
		return tablePanel;
	}

	private JPanel createVagasTab() {
		JPanel tablePanel = new JPanel(new BorderLayout(0, 12));
		tablePanel.setBackground(Color.WHITE);
		tablePanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(222, 228, 235)),
			BorderFactory.createEmptyBorder(16, 16, 16, 16)
		));

		JLabel tableTitle = new JLabel("Vagas de Estacionamento");
		tableTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
		tableTitle.setForeground(TEXT);
		tablePanel.add(tableTitle, BorderLayout.NORTH);

		String[] columns = {"Vaga (spotName)", "Andar (floor)", "Tipo (type)", "Status", "Placa (licensePlate)"};
		DefaultTableModel model = new DefaultTableModel(null, columns) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		table_vagas = createStyledTable(model);

		DefaultTableCellRenderer textRenderer = createTextRenderer();
		for (int column = 0; column < columns.length; column++) {
			if (column != 3) {
				table_vagas.getColumnModel().getColumn(column).setCellRenderer(textRenderer);
			}
		}
		table_vagas.getColumnModel().getColumn(3).setCellRenderer(new VagaStatusRenderer());

		JScrollPane scrollPane = new JScrollPane(table_vagas);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(222, 228, 235)));
		scrollPane.getViewport().setBackground(Color.WHITE);
		tablePanel.add(scrollPane, BorderLayout.CENTER);
		return tablePanel;
	}

	private JPanel createVehiclesTab() {
		JPanel tablePanel = new JPanel(new BorderLayout(0, 12));
		tablePanel.setBackground(Color.WHITE);
		tablePanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(222, 228, 235)),
			BorderFactory.createEmptyBorder(16, 16, 16, 16)
		));

		JLabel tableTitle = new JLabel("Veículos Cadastrados");
		tableTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
		tableTitle.setForeground(TEXT);
		tablePanel.add(tableTitle, BorderLayout.NORTH);

		String[] columns = {"Placa (licensePlate)", "Marca (brand)", "Modelo (model)", "Tipo (type)", "Proprietário (username)"};
		DefaultTableModel model = new DefaultTableModel(null, columns) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		table_vehicles = createStyledTable(model);

		DefaultTableCellRenderer textRenderer = createTextRenderer();
		for (int column = 0; column < columns.length; column++) {
			table_vehicles.getColumnModel().getColumn(column).setCellRenderer(textRenderer);
		}

		JScrollPane scrollPane = new JScrollPane(table_vehicles);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(222, 228, 235)));
		scrollPane.getViewport().setBackground(Color.WHITE);
		tablePanel.add(scrollPane, BorderLayout.CENTER);
		return tablePanel;
	}

	private JTable createStyledTable(DefaultTableModel model) {
		JTable table = new JTable(model);
		table.setFont(new Font("SansSerif", Font.PLAIN, 13));
		table.setForeground(TEXT);
		table.setRowHeight(32);
		table.setShowVerticalLines(false);
		table.setShowHorizontalLines(true);
		table.setGridColor(new Color(232, 236, 241));
		table.setIntercellSpacing(new java.awt.Dimension(0, 1));
		table.setSelectionBackground(new Color(220, 235, 250));
		table.setSelectionForeground(TEXT);
		table.setFillsViewportHeight(true);
		table.setAutoCreateRowSorter(true);
		return table;
	}

	private DefaultTableCellRenderer createTextRenderer() {
		DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer();
		textRenderer.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		return textRenderer;
	}

	private void configureUserColumnWidths(TableColumnModel columns) {
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

	// ==================== AÇÕES DOS BOTÕES ====================

	private void acaoAdicionar() {
		int aba = tabbedPane.getSelectedIndex();
		if (aba == 0) {
			adicionarUsuarioDialog();
		} else if (aba == 1) {
			adicionarVagaDialog();
		} else if (aba == 2) {
			adicionarVeiculoDialog();
		}
	}

	private void acaoAtualizar() {
		int aba = tabbedPane.getSelectedIndex();
		if (aba == 0) {
			atualizarUsuarioDialog();
		} else if (aba == 1) {
			atualizarVagaDialog();
		} else if (aba == 2) {
			atualizarVeiculoDialog();
		}
	}

	private void acaoApagar() {
		int aba = tabbedPane.getSelectedIndex();
		if (aba == 0) {
			apagarUsuarioDialog();
		} else if (aba == 1) {
			apagarVagaDialog();
		} else if (aba == 2) {
			apagarVeiculoDialog();
		}
	}

	// --- Diálogos de Usuário (register, updateusername, updateuserpassword, deleteuser) ---

	private void adicionarUsuarioDialog() {
		JTextField txtName = new JTextField();
		JTextField txtUsername = new JTextField();
		JPasswordField txtPassword = new JPasswordField();
		JComboBox<String> cbRole = new JComboBox<>(new String[] {"user", "admin"});

		JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
		panel.add(new JLabel("Nome completo:"));
		panel.add(txtName);
		panel.add(new JLabel("Username (3-20 caracteres, minúsculas/números/._):"));
		panel.add(txtUsername);
		panel.add(new JLabel("Senha (8-20 caracteres, maiúscula, minúscula, número e especial):"));
		panel.add(txtPassword);
		panel.add(new JLabel("Papel (role):"));
		panel.add(cbRole);

		int result = JOptionPane.showConfirmDialog(this, panel, "Adicionar Usuário (register)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			String name = txtName.getText().trim();
			String username = txtUsername.getText().trim();
			String password = new String(txtPassword.getPassword()).trim();
			String role = (String) cbRole.getSelectedItem();

			JsonObject data = new JsonObject();
			data.addProperty("name", name);
			data.addProperty("username", username);
			data.addProperty("password", password);
			data.addProperty("role", role);
			data.addProperty("token", Session.SERVER_ADMIN_TOKEN);

			JsonObject req = new JsonObject();
			req.addProperty("method", "register");
			req.add("data", data);

			executarOperacao("Criando usuário", () -> userService.register(req));
		}
	}

	private void atualizarUsuarioDialog() {
		int viewRow = table_users.getSelectedRow();
		if (viewRow < 0) {
			JOptionPane.showMessageDialog(this, "Selecione um usuário na tabela para atualizar.", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int modelRow = table_users.convertRowIndexToModel(viewRow);
		String currentName = String.valueOf(table_users.getModel().getValueAt(modelRow, 0));
		String username = String.valueOf(table_users.getModel().getValueAt(modelRow, 1));

		JTextField txtName = new JTextField(currentName);
		JPasswordField txtNewPassword = new JPasswordField();

		JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
		panel.add(new JLabel("Usuário: " + username));
		panel.add(new JLabel("Novo Nome:"));
		panel.add(txtName);
		panel.add(new JLabel("Nova Senha (deixe em branco para manter a atual):"));
		panel.add(txtNewPassword);

		int result = JOptionPane.showConfirmDialog(this, panel, "Atualizar Usuário (" + username + ")", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			String newName = txtName.getText().trim();
			String newPass = new String(txtNewPassword.getPassword()).trim();

			executarOperacao("Atualizando usuário", () -> {
				JsonObject dataName = new JsonObject();
				dataName.addProperty("token", Session.SERVER_ADMIN_TOKEN);
				dataName.addProperty("username", username);
				dataName.addProperty("name", newName);

				JsonObject reqName = new JsonObject();
				reqName.addProperty("method", "updateusername");
				reqName.add("data", dataName);

				JsonObject res = userService.updateUserName(reqName);
				int code = res.has("statusCode") ? res.get("statusCode").getAsInt() : 500;
				if (code != 200) {
					return res;
				}

				if (!newPass.isEmpty()) {
					JsonObject dataPass = new JsonObject();
					dataPass.addProperty("token", Session.SERVER_ADMIN_TOKEN);
					dataPass.addProperty("username", username);
					dataPass.addProperty("newPassword", newPass);

					JsonObject reqPass = new JsonObject();
					reqPass.addProperty("method", "updateuserpassword");
					reqPass.add("data", dataPass);
					return userService.updateUserPassword(reqPass);
				}
				return res;
			});
		}
	}

	private void apagarUsuarioDialog() {
		int viewRow = table_users.getSelectedRow();
		if (viewRow < 0) {
			JOptionPane.showMessageDialog(this, "Selecione um usuário na tabela para apagar.", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int modelRow = table_users.convertRowIndexToModel(viewRow);
		String username = String.valueOf(table_users.getModel().getValueAt(modelRow, 1));

		int confirm = JOptionPane.showConfirmDialog(this,
				"Deseja realmente remover o usuário '" + username + "'?",
				"Confirmar Exclusão (deleteuser)",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);

		if (confirm == JOptionPane.YES_OPTION) {
			JsonObject data = new JsonObject();
			data.addProperty("token", Session.SERVER_ADMIN_TOKEN);
			data.addProperty("username", username);

			JsonObject req = new JsonObject();
			req.addProperty("method", "deleteuser");
			req.add("data", data);

			executarOperacao("Removendo usuário", () -> userService.deleteUser(req));
		}
	}

	// --- Diálogos de Vaga (createvaga, updatevaga, deletevaga, findvehicle) ---

	private void adicionarVagaDialog() {
		JTextField txtSpotName = new JTextField();
		JTextField txtFloor = new JTextField("1");
		JComboBox<String> cbType = new JComboBox<>(new String[] {"combustao", "eletrico"});

		JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
		panel.add(new JLabel("Identificador da Vaga (spotName):"));
		panel.add(txtSpotName);
		panel.add(new JLabel("Andar (floor):"));
		panel.add(txtFloor);
		panel.add(new JLabel("Tipo (type):"));
		panel.add(cbType);

		int result = JOptionPane.showConfirmDialog(this, panel, "Adicionar Vaga (createvaga)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			String spotName = txtSpotName.getText().trim();
			String floorStr = txtFloor.getText().trim();
			String type = (String) cbType.getSelectedItem();

			int floor;
			try {
				floor = Integer.parseInt(floorStr);
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(this, "O andar (floor) deve ser um número inteiro.", "Formato Inválido", JOptionPane.WARNING_MESSAGE);
				return;
			}

			JsonObject data = new JsonObject();
			data.addProperty("token", Session.SERVER_ADMIN_TOKEN);
			data.addProperty("spotName", spotName);
			data.addProperty("floor", floor);
			data.addProperty("type", type);

			JsonObject req = new JsonObject();
			req.addProperty("method", "createvaga");
			req.add("data", data);

			executarOperacao("Criando vaga", () -> vagaService.createVaga(req));
		}
	}

	private void atualizarVagaDialog() {
		int viewRow = table_vagas.getSelectedRow();
		if (viewRow < 0) {
			JOptionPane.showMessageDialog(this, "Selecione uma vaga na tabela para atualizar.", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int modelRow = table_vagas.convertRowIndexToModel(viewRow);
		String currentSpotName = String.valueOf(table_vagas.getModel().getValueAt(modelRow, 0));
		String currentFloor = String.valueOf(table_vagas.getModel().getValueAt(modelRow, 1));
		String currentType = String.valueOf(table_vagas.getModel().getValueAt(modelRow, 2));
		String currentStatus = String.valueOf(table_vagas.getModel().getValueAt(modelRow, 3));
		String currentPlate = String.valueOf(table_vagas.getModel().getValueAt(modelRow, 4));
		if ("-".equals(currentPlate)) currentPlate = "";

		JTextField txtNewSpotName = new JTextField(currentSpotName);
		JTextField txtFloor = new JTextField(currentFloor);
		JComboBox<String> cbType = new JComboBox<>(new String[] {"combustao", "eletrico"});
		cbType.setSelectedItem(currentType);
		JComboBox<String> cbStatus = new JComboBox<>(new String[] {"disponivel", "ocupada", "fora_de_servico"});
		cbStatus.setSelectedItem(currentStatus);
		JTextField txtPlate = new JTextField(currentPlate);

		JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
		panel.add(new JLabel("Vaga atual: " + currentSpotName));
		panel.add(new JLabel("Novo nome da vaga (newSpotName):"));
		panel.add(txtNewSpotName);
		panel.add(new JLabel("Andar (floor):"));
		panel.add(txtFloor);
		panel.add(new JLabel("Tipo (type):"));
		panel.add(cbType);
		panel.add(new JLabel("Status:"));
		panel.add(cbStatus);
		panel.add(new JLabel("Placa do Veículo (obrigatória se status = ocupada):"));
		panel.add(txtPlate);

		int result = JOptionPane.showConfirmDialog(this, panel, "Atualizar Vaga (updatevaga)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			String newSpotName = txtNewSpotName.getText().trim();
			String floorStr = txtFloor.getText().trim();
			String type = (String) cbType.getSelectedItem();
			String status = (String) cbStatus.getSelectedItem();
			String plate = txtPlate.getText().trim();

			int floor;
			try {
				floor = Integer.parseInt(floorStr);
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(this, "O andar (floor) deve ser um número inteiro.", "Formato Inválido", JOptionPane.WARNING_MESSAGE);
				return;
			}

			JsonObject data = new JsonObject();
			data.addProperty("token", Session.SERVER_ADMIN_TOKEN);
			data.addProperty("spotName", currentSpotName);
			if (!newSpotName.isEmpty() && !newSpotName.equals(currentSpotName)) {
				data.addProperty("newSpotName", newSpotName);
			}
			data.addProperty("floor", floor);
			data.addProperty("type", type);
			data.addProperty("status", status);
			if (!plate.isEmpty()) {
				data.addProperty("licensePlate", plate);
			}

			JsonObject req = new JsonObject();
			req.addProperty("method", "updatevaga");
			req.add("data", data);

			executarOperacao("Atualizando vaga", () -> vagaService.updateVaga(req));
		}
	}

	private void apagarVagaDialog() {
		int viewRow = table_vagas.getSelectedRow();
		if (viewRow < 0) {
			JOptionPane.showMessageDialog(this, "Selecione uma vaga na tabela para apagar.", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int modelRow = table_vagas.convertRowIndexToModel(viewRow);
		String spotName = String.valueOf(table_vagas.getModel().getValueAt(modelRow, 0));

		int confirm = JOptionPane.showConfirmDialog(this,
				"Deseja realmente remover a vaga '" + spotName + "'?",
				"Confirmar Exclusão (deletevaga)",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);

		if (confirm == JOptionPane.YES_OPTION) {
			JsonObject data = new JsonObject();
			data.addProperty("token", Session.SERVER_ADMIN_TOKEN);
			data.addProperty("spotName", spotName);

			JsonObject req = new JsonObject();
			req.addProperty("method", "deletevaga");
			req.add("data", data);

			executarOperacao("Removendo vaga", () -> vagaService.deleteVaga(req));
		}
	}

	private void acaoLocalizarVeiculo() {
		String plate = JOptionPane.showInputDialog(this, "Informe a placa (licensePlate) do veículo:", "Localizar Veículo (findvehicle / getvehicle)", JOptionPane.QUESTION_MESSAGE);
		if (plate == null || plate.trim().isEmpty()) {
			return;
		}
		String licensePlate = plate.trim();

		setLoading(true, "Localizando veículo " + licensePlate + "...");
		new SwingWorker<Void, Void>() {
			private JsonObject resSpot;
			private JsonObject resVehicle;

			@Override
			protected Void doInBackground() {
				JsonObject data = new JsonObject();
				data.addProperty("token", Session.SERVER_ADMIN_TOKEN);
				data.addProperty("licensePlate", licensePlate);

				JsonObject reqFind = new JsonObject();
				reqFind.addProperty("method", "findvehicle");
				reqFind.add("data", data);
				resSpot = vagaService.findVehicle(reqFind);

				JsonObject reqGet = new JsonObject();
				reqGet.addProperty("method", "getvehicle");
				reqGet.add("data", data);
				resVehicle = vehicleService.getVehicle(reqGet);
				return null;
			}

			@Override
			protected void done() {
				setLoading(false, "Busca concluída");
				StringBuilder sb = new StringBuilder();
				sb.append("Placa consultada: ").append(licensePlate).append("\n\n");

				if (resVehicle != null && resVehicle.has("statusCode") && resVehicle.get("statusCode").getAsInt() == 200
						&& resVehicle.has("data") && !resVehicle.get("data").isJsonNull()) {
					JsonObject vData = resVehicle.getAsJsonObject("data");
					sb.append("Dados do Veículo:\n");
					sb.append(" - Marca: ").append(vData.get("brand").getAsString()).append("\n");
					sb.append(" - Modelo: ").append(vData.get("model").getAsString()).append("\n");
					sb.append(" - Tipo: ").append(vData.get("type").getAsString()).append("\n\n");
				} else {
					sb.append("Dados do Veículo: Não encontrado no cadastro.\n\n");
				}

				if (resSpot != null && resSpot.has("statusCode") && resSpot.get("statusCode").getAsInt() == 200
						&& resSpot.has("data") && !resSpot.get("data").isJsonNull()) {
					JsonObject sData = resSpot.getAsJsonObject("data");
					sb.append("Localização na Garagem (findvehicle):\n");
					sb.append(" - Vaga (spotName): ").append(sData.get("spotName").getAsString()).append("\n");
					sb.append(" - Andar (floor): ").append(sData.get("floor").getAsInt()).append("\n");
					sb.append(" - Status: ").append(sData.get("status").getAsString());
				} else {
					String msg = resSpot != null && resSpot.has("message") ? resSpot.get("message").getAsString() : "Não estacionado.";
					sb.append("Localização na Garagem: ").append(msg);
				}

				JOptionPane.showMessageDialog(HomeWindow.this, sb.toString(), "Resultado da Busca", JOptionPane.INFORMATION_MESSAGE);
			}
		}.execute();
	}

	// --- Diálogos de Veículo (createvehicle, updatevehicle, deletevehicle) ---

	private void adicionarVeiculoDialog() {
		List<User> users = sessionService.getAllUsers();
		JComboBox<String> cbOwner = new JComboBox<>();
		cbOwner.setEditable(true);
		if (users != null) {
			for (User u : users) {
				cbOwner.addItem(u.getUsername());
			}
		}

		JTextField txtPlate = new JTextField();
		JTextField txtBrand = new JTextField();
		JTextField txtModel = new JTextField();
		JComboBox<String> cbType = new JComboBox<>(new String[] {"combustao", "eletrico"});

		JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
		panel.add(new JLabel("Proprietário (username):"));
		panel.add(cbOwner);
		panel.add(new JLabel("Placa (licensePlate):"));
		panel.add(txtPlate);
		panel.add(new JLabel("Marca (brand):"));
		panel.add(txtBrand);
		panel.add(new JLabel("Modelo (model):"));
		panel.add(txtModel);
		panel.add(new JLabel("Tipo (type):"));
		panel.add(cbType);

		int result = JOptionPane.showConfirmDialog(this, panel, "Adicionar Veículo (createvehicle)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			Object selectedUser = cbOwner.getSelectedItem();
			String owner = selectedUser != null ? selectedUser.toString().trim() : "";
			String plate = txtPlate.getText().trim();
			String brand = txtBrand.getText().trim();
			String model = txtModel.getText().trim();
			String type = (String) cbType.getSelectedItem();

			JsonObject data = new JsonObject();
			data.addProperty("token", Session.SERVER_ADMIN_TOKEN);
			data.addProperty("username", owner);
			data.addProperty("licensePlate", plate);
			data.addProperty("brand", brand);
			data.addProperty("model", model);
			data.addProperty("type", type);

			JsonObject req = new JsonObject();
			req.addProperty("method", "createvehicle");
			req.add("data", data);

			executarOperacao("Cadastrando veículo", () -> vehicleService.createVehicle(req));
		}
	}

	private void atualizarVeiculoDialog() {
		int viewRow = table_vehicles.getSelectedRow();
		if (viewRow < 0) {
			JOptionPane.showMessageDialog(this, "Selecione um veículo na tabela para atualizar.", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int modelRow = table_vehicles.convertRowIndexToModel(viewRow);
		String licensePlate = String.valueOf(table_vehicles.getModel().getValueAt(modelRow, 0));
		String currentBrand = String.valueOf(table_vehicles.getModel().getValueAt(modelRow, 1));
		String currentModel = String.valueOf(table_vehicles.getModel().getValueAt(modelRow, 2));
		String currentType = String.valueOf(table_vehicles.getModel().getValueAt(modelRow, 3));

		JTextField txtBrand = new JTextField(currentBrand);
		JTextField txtModel = new JTextField(currentModel);
		JComboBox<String> cbType = new JComboBox<>(new String[] {"combustao", "eletrico"});
		cbType.setSelectedItem(currentType);

		JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
		panel.add(new JLabel("Placa (licensePlate): " + licensePlate));
		panel.add(new JLabel("Marca (brand):"));
		panel.add(txtBrand);
		panel.add(new JLabel("Modelo (model):"));
		panel.add(txtModel);
		panel.add(new JLabel("Tipo (type):"));
		panel.add(cbType);

		int result = JOptionPane.showConfirmDialog(this, panel, "Atualizar Veículo (updatevehicle)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			String brand = txtBrand.getText().trim();
			String model = txtModel.getText().trim();
			String type = (String) cbType.getSelectedItem();

			JsonObject data = new JsonObject();
			data.addProperty("token", Session.SERVER_ADMIN_TOKEN);
			data.addProperty("licensePlate", licensePlate);
			data.addProperty("brand", brand);
			data.addProperty("model", model);
			data.addProperty("type", type);

			JsonObject req = new JsonObject();
			req.addProperty("method", "updatevehicle");
			req.add("data", data);

			executarOperacao("Atualizando veículo", () -> vehicleService.updateVehicle(req));
		}
	}

	private void apagarVeiculoDialog() {
		int viewRow = table_vehicles.getSelectedRow();
		if (viewRow < 0) {
			JOptionPane.showMessageDialog(this, "Selecione um veículo na tabela para apagar.", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int modelRow = table_vehicles.convertRowIndexToModel(viewRow);
		String licensePlate = String.valueOf(table_vehicles.getModel().getValueAt(modelRow, 0));

		int confirm = JOptionPane.showConfirmDialog(this,
				"Deseja realmente remover o veículo de placa '" + licensePlate + "'?",
				"Confirmar Exclusão (deletevehicle)",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);

		if (confirm == JOptionPane.YES_OPTION) {
			JsonObject data = new JsonObject();
			data.addProperty("token", Session.SERVER_ADMIN_TOKEN);
			data.addProperty("licensePlate", licensePlate);

			JsonObject req = new JsonObject();
			req.addProperty("method", "deletevehicle");
			req.add("data", data);

			executarOperacao("Removendo veículo", () -> vehicleService.deleteVehicle(req));
		}
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

class VagaStatusRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;
	private static final Color DISPONIVEL = new Color(31, 143, 86);
	private static final Color OCUPADA = new Color(36, 95, 156);
	private static final Color FORA_DE_SERVICO = new Color(190, 63, 63);

	@Override
	public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		JLabel label = (JLabel) super.getTableCellRendererComponent(
			table, value, isSelected, hasFocus, row, column);

		String status = String.valueOf(value);
		Color statusColor;
		if ("disponivel".equalsIgnoreCase(status)) {
			statusColor = DISPONIVEL;
		} else if ("ocupada".equalsIgnoreCase(status)) {
			statusColor = OCUPADA;
		} else {
			statusColor = FORA_DE_SERVICO;
		}
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
