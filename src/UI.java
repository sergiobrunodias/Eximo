import java.awt.CardLayout;
import java.awt.Container;

import javax.swing.JFrame;

public class UI extends JFrame {
	private GamePanel gamePanel;
	private MenuPanel menuPanel;
	private CardLayout layout;
	
	public UI() {
		super("gui");
		
		setVisible(true);
		setSize(500,700);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setTitle("Eximo");
		
		//Set layout manager
		layout = new CardLayout();
		setLayout(layout);
		
		gamePanel = new GamePanel();
		menuPanel = new MenuPanel();
		
		//Add Swing components to content pane
		Container c = getContentPane();
		c.add(menuPanel);
		c.add(gamePanel);
		
	}
	
	public void setMenuListener(Controller ctrl) {
		menuPanel.setButtonListeners(ctrl);
	}
	
	public void displayGamePanel() {
		layout.next(getContentPane());
	}
	
	public Board getBoard() {
		return gamePanel.getBoard();
	}
}
