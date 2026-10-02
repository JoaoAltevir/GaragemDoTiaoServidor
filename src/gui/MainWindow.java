package gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import service.*;

public class MainWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField textField;
	private HomeWindow home;


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
		setBounds(100, 100, 490, 345);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lbl_portAddress = new JLabel("Qual porta deseja usar?");
		lbl_portAddress.setBounds(166, 84, 191, 14);
		contentPane.add(lbl_portAddress);
		
		textField = new JTextField();
		textField.setBounds(164, 109, 114, 20);
		contentPane.add(textField);
		textField.setColumns(10);
		
		JButton btn_init = new JButton("Iniciar");
		btn_init.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				abrirHome(Integer.parseInt(textField.getText()));
			}
		});
		btn_init.setBounds(174, 140, 89, 23);
		contentPane.add(btn_init);
	}
}
