package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.JButton;

import service.*;

public class HomeWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTable table_users;
	private JButton btn_add;
	private JButton btn_delete;
	private JButton btn_exit;
	private JButton btn_update;
	//SERVICES
	private ServerService serverService;
	//GUI's
	private MainWindow mainWindow;
	

	/**
	 * Create the frame.
	 */
	public HomeWindow(int port) {

		this.serverService = new ServerService(port, this);
		initComponents();
		this.serverService.iniciarServidor();
		
	}
	
	public void initComponents() {
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 562, 406);
		setTitle("Servidor");
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		getContentPane().setLayout(null);
		JScrollPane scrollPane_users = new JScrollPane();
		scrollPane_users.setBounds(44, 43, 442, 247);
		getContentPane().add(scrollPane_users);
		
		table_users = new JTable();
		scrollPane_users.setViewportView(table_users);
		String[] colunas = {"Nome do usuário", "IP", "Role", "Status"};
		DefaultTableModel model = new DefaultTableModel(null, colunas);
		table_users.setModel(model);
		
		btn_add = new JButton("Adicionar");
		btn_add.setBounds(397, 299, 89, 23);
		contentPane.add(btn_add);
		
		btn_delete = new JButton("Apagar");
		btn_delete.setBounds(306, 299, 89, 23);
		contentPane.add(btn_delete);
		
		btn_exit = new JButton("Sair");
		btn_exit.setBounds(10, 333, 89, 23);
		contentPane.add(btn_exit);
		
		btn_update = new JButton("Atualizar");
		btn_update.setBounds(216, 299, 89, 23);
		contentPane.add(btn_update);
		
		table_users.getColumnModel().getColumn(3).setCellRenderer(new StatusRenderer());
		
	}
}

class StatusRenderer extends DefaultTableCellRenderer {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        // Pega o componente padrão (que é um JLabel)
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        String status = (String) value;
        
        if ("Online".equalsIgnoreCase(status)) {
            // Bolinha verde + texto verde
            label.setIcon(new BolinhaIcon(new Color(46, 204, 113))); 
            label.setForeground(new Color(46, 204, 113));
        } else {
            // Bolinha vermelha + texto vermelho
            label.setIcon(new BolinhaIcon(new Color(231, 76, 60))); 
            label.setForeground(new Color(231, 76, 60));
        }
        
        // Centraliza o conteúdo na célula
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }
}

/**
 * Desenha uma bolinha colorida dinamicamente para servir de ícone
 */
class BolinhaIcon implements Icon {
    private Color color;

    public BolinhaIcon(Color color) {
        this.color = color;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        // Desenha a bolinha (x, y, largura, altura)
        g2.fillOval(x, y + 2, 10, 10);
        g2.dispose();
    }

    public int getIconWidth() { return 15; }

    public int getIconHeight() { return 15; }
}
