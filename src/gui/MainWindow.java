package gui;

import java.awt.EventQueue;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;


public class MainWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private JTextField textField;
	private HomeWindow home;
	private static final Color BACKGROUND = new Color(245, 247, 250);
	private static final Color TEXT = new Color(35, 45, 58);
	private static final Color MUTED_TEXT = new Color(100, 112, 128);
	private static final Color ACCENT = new Color(36, 95, 156);


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow frame = new MainWindow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */

	public MainWindow() {

		initComponents();

	}

	
	public void abrirHome(int port) {
		
		this.home = new HomeWindow(port, this);
		home.setVisible(true);
		this.setVisible(false);
		
	}
	
	public void initComponents() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Garagem do Tiao - Servidor");
		setSize(520, 360);
		setMinimumSize(new Dimension(440, 320));
		setLocationRelativeTo(null);

		JPanel contentPane = new JPanel(new GridBagLayout());
		contentPane.setBackground(BACKGROUND);
		contentPane.setBorder(new EmptyBorder(24, 28, 24, 28));
		setContentPane(contentPane);

		JPanel card = new JPanel(new GridBagLayout());
		card.setBackground(Color.WHITE);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(222, 228, 235)),
			new EmptyBorder(28, 32, 28, 32)
		));

		GridBagConstraints cardConstraints = new GridBagConstraints();
		cardConstraints.gridx = 0;
		cardConstraints.weightx = 1;
		cardConstraints.fill = GridBagConstraints.HORIZONTAL;
		cardConstraints.insets = new Insets(0, 0, 10, 0);

		JLabel title = new JLabel("Iniciar servidor", SwingConstants.CENTER);
		title.setFont(new Font("SansSerif", Font.BOLD, 23));
		title.setForeground(TEXT);
		cardConstraints.gridy = 0;
		cardConstraints.insets = new Insets(0, 0, 8, 0);
		card.add(title, cardConstraints);

		JLabel description = new JLabel("Informe a porta para aceitar conexoes", SwingConstants.CENTER);
		description.setFont(new Font("SansSerif", Font.PLAIN, 14));
		description.setForeground(MUTED_TEXT);
		cardConstraints.gridy = 1;
		cardConstraints.insets = new Insets(0, 0, 22, 0);
		card.add(description, cardConstraints);

		JLabel lbl_portAddress = new JLabel("Porta", SwingConstants.LEFT);
		lbl_portAddress.setFont(new Font("SansSerif", Font.BOLD, 13));
		lbl_portAddress.setForeground(TEXT);
		cardConstraints.gridy = 2;
		cardConstraints.insets = new Insets(0, 0, 7, 0);
		card.add(lbl_portAddress, cardConstraints);

		textField = new JTextField();
		textField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		textField.setColumns(12);
		textField.setPreferredSize(new Dimension(180, 36));
		textField.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(205, 213, 222)),
			BorderFactory.createEmptyBorder(6, 9, 6, 9)
		));
		cardConstraints.gridy = 3;
		cardConstraints.insets = new Insets(0, 0, 18, 0);
		card.add(textField, cardConstraints);

		JButton btn_init = new JButton("Iniciar");
		btn_init.setFont(new Font("SansSerif", Font.BOLD, 14));
		btn_init.setForeground(Color.WHITE);
		btn_init.setBackground(ACCENT);
		btn_init.setFocusPainted(false);
		btn_init.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
		btn_init.addActionListener(e -> abrirHome(Integer.parseInt(textField.getText())));
		cardConstraints.gridy = 4;
		cardConstraints.fill = GridBagConstraints.NONE;
		cardConstraints.anchor = GridBagConstraints.CENTER;
		cardConstraints.insets = new Insets(0, 0, 0, 0);
		card.add(btn_init, cardConstraints);

		GridBagConstraints contentConstraints = new GridBagConstraints();
		contentConstraints.fill = GridBagConstraints.HORIZONTAL;
		contentConstraints.weightx = 1;
		contentConstraints.insets = new Insets(0, 0, 0, 0);
		contentPane.add(card, contentConstraints);
	}
}
