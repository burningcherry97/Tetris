import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

class Shape
{
	private static final int[][][] coos={
		{{-1,0},{0,0},{1,0},{2,0}},		// I
		{{-1,0},{0,0},{1,0},{0,1}},		// T
		{{0,0},{1,0},{0,1},{1,1}},		// O
		{{-1,0},{-1,1},{0,0},{1,0}},	// L
		{{-1,0},{0,0},{1,0},{1,1}},		// J
		{{-1,1},{0,1},{0,0},{1,0}},		// S
		{{-1,0},{0,0},{0,1},{1,1}}		// Z
	};
	private static Random r =new Random();

	public int[] x =new int[4];
	public int[] y =new int[4];
	private int typ;

	public int getType()
	{
		return typ;
	}

	public void rotate()
	{
		if (typ==Board.O_SHAPE) return;

		int a;

		for (int i=0; i<4; i++)
		{
			a=-1*y[i];
			y[i]=x[i];
			x[i]=a;
		}
	}

	public Shape()
	{
		typ = Math.abs(r.nextInt())%7;
		// System.out.println("tworzenie kształtu... typ: "+type);
		for (int i=0; i<4; i++)
		{
			x[i]=coos[typ][i][0];
			y[i]=coos[typ][i][1];
		}
	}
}

class Board extends JFrame implements ActionListener
{
	private int[][] field;
	private boolean[][] placed;
	private int Height, Width, Speed;
	private int score;
	private FieldPanel CellField;
	private JLabel ScoreLabel;
	private javax.swing.Timer timer;
	private Shape CurrentShape;
	private int ShapeCenterX, ShapeCenterY;
	private boolean paused;

	public static final int EMPTY=-1, I_SHAPE=0, T_SHAPE=1, O_SHAPE=2, L_SHAPE=3, J_SHAPE=4, S_SHAPE=5, Z_SHAPE=6;

	public Board(int height, int width, int speed)
	{
		super("Tetris");
		setSize(300,300);
		setLocation(300,0);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		field =new int[height][width];
		for (int i=0; i<height; i++) Arrays.fill(field[i],EMPTY);
		// for (int i=0; i<height; i++)
		// {
		// 	for (int j=0; j<width; j++)
		// 	{
		// 		field[i][j]=EMPTY;
		// 	}
		// }

		placed =new boolean[height][width];

		CellField =new FieldPanel(height,width);
		ScoreLabel =new JLabel("0",JLabel.RIGHT);

		add(CellField,BorderLayout.CENTER);
		add(ScoreLabel,BorderLayout.SOUTH);

		this.Height=height;
		this.Width=width;
		this.Speed=speed;

		timer =new javax.swing.Timer((10-Speed)*100,this);
		score=0;
		paused=false;

		pack();
		setResizable(false);

		start();
	}

	private void start()
	{
		timer.start();
		nextShape();
		addKeyListener(new TetrisAdapter());
	}

	private synchronized void step()
	{
		lineDown();
		repaint();
		summarize();
	}

	private void lineDown()
	{
		if (checkMoveable("down"))
		{
			clearShape();
			placeCurrentShape(ShapeCenterX,++ShapeCenterY);
		}
		else 
		{
			nextShape();
		}
	}

	private void nextShape()
	{
		if (CurrentShape!=null)
		{
			markPlaced();
		}
		CurrentShape =new Shape();
		ShapeCenterX=Width/2;
		ShapeCenterY=1;
		if (checkMoveable("insert"))
		{
			placeCurrentShape(ShapeCenterX,ShapeCenterY);
		}
		else
		{
			end();
		}
	}

	private synchronized void moveRight()
	{
		if (checkMoveable("right")&&!paused)
		{
			clearShape();
			placeCurrentShape(++ShapeCenterX,ShapeCenterY);
			repaint();
		}
	}

	private synchronized void moveLeft()
	{
		if (checkMoveable("left")&&!paused)
		{
			clearShape();
			placeCurrentShape(--ShapeCenterX,ShapeCenterY);
			repaint();
		}
	}

	private synchronized void rotate()
	{
		if (checkMoveable("rotate")&&!paused)
		{
			clearShape();
			CurrentShape.rotate();
			placeCurrentShape(ShapeCenterX,ShapeCenterY);
			repaint();
		}
	}

	private synchronized void let_fall()
	{
		while (checkMoveable("down")&&!paused)
		{
			lineDown();
		}
		nextShape();
		repaint();
	}

	private synchronized void summarize()
	{
		int LinesRemoved = 0;
		for (int i=Height-1; i>0; --i)
		{
			if (checkFull(i))
			{
				for (int k=i; k>0; --k)
				{
					for (int j=0; j<Width; j++)
					{
						if (replaceable(k,j)) field[k][j]=field[k-1][j];
						placed[k][j]=placed[k-1][j];
					}
				}
				for (int j=0; j<Width; j++)
				{
					field[0][j]=EMPTY;
					placed[0][j]=false;;
				}
				LinesRemoved++;
				i++;
			}
		}
		score+=Math.pow(LinesRemoved*(Speed+11),2)*100*Width;
		updateScore(true);
	}

	private void updateScore(boolean not_lost)
	{
		if (not_lost)
		{
			ScoreLabel.setText(""+score);
		}
		else
		{
			System.out.println("koniec");
			ScoreLabel.setVerticalAlignment(JLabel.CENTER);
			ScoreLabel.setText("Koniec gry. Wynik: "+score+" punktów");
		}
	}

	private boolean checkFull(int i)
	{
		for (int j=0; j<Width; j++)
		{
			if (!placed[i][j]) return false;
		}
		return true;
	}

	private boolean checkMoveable(String mode)
	{
		if (mode.equals("down"))
		{
			for (int i=0; i<4; i++)
			{
				if (ShapeCenterY+CurrentShape.y[i]+1==Height)
				{
					return false;
				}
				if (placed[ShapeCenterY+CurrentShape.y[i]+1][ShapeCenterX+CurrentShape.x[i]]) return false;
			}
			return true;
		}
		else if (mode.equals("insert"))
		{
			for (int i=0; i<4; i++)
			{
				if (placed[ShapeCenterY+CurrentShape.y[i]][ShapeCenterX+CurrentShape.x[i]]) return false;
			}
			return true;
		}
		else if (mode.equals("right"))
		{
			for (int i=0; i<4; i++)
			{
				if (ShapeCenterX+CurrentShape.x[i]+1==Width)
				{
					return false;
				}
				if (placed[ShapeCenterY+CurrentShape.y[i]][ShapeCenterX+CurrentShape.x[i]+1]) return false;
			}
			return true;
		}
		else if (mode.equals("left"))
		{
			for (int i=0; i<4; i++)
			{
				if (ShapeCenterX+CurrentShape.x[i]-1<0)
				{
					return false;
				}
				if (placed[ShapeCenterY+CurrentShape.y[i]][ShapeCenterX+CurrentShape.x[i]-1]) return false;
			}
			return true;
		}
		else if (mode.equals("rotate"))
		{
			for (int i=0; i<4; i++)
			{
				if (ShapeCenterX-CurrentShape.y[i]<0) return false;
				if (ShapeCenterX-CurrentShape.y[i]==Width) return false;
				if (ShapeCenterX+CurrentShape.x[i]<0) return false;
				if (ShapeCenterX+CurrentShape.x[i]==Height) return false;
				try{
					if (placed[ShapeCenterY+CurrentShape.x[i]][ShapeCenterX-CurrentShape.y[i]]) return false;
				}
				catch (Exception xD)
				{
					return false;
				}
			}
			return true;
		}
		else
		{
			return true;
		}
	}

	private boolean replaceable(int k, int j)
	{
		try{
			if (placed[k][j]) return true;
			else return replaceable(k-1,j);
		}
		catch (Exception xD)
		{
			return false;
		}
	}

	private void placeCurrentShape(int x, int y)
	{
		for (int i=0; i<4; i++)
		{
			field[y+CurrentShape.y[i]][x+CurrentShape.x[i]]=CurrentShape.getType();
		}
	}

	private void clearShape()
	{
		for (int i=0; i<4; i++)
		{
			field[ShapeCenterY+CurrentShape.y[i]][ShapeCenterX+CurrentShape.x[i]]=EMPTY;
		}
	}

	private void markPlaced()
	{
		for (int i=0; i<4; i++)
		{
			placed[ShapeCenterY+CurrentShape.y[i]][ShapeCenterX+CurrentShape.x[i]]=true;
		}
	}

	private void print()
	{
		for (int i=0; i<Height; i++)
		{
			for (int j=0; j<Width; j++)
			{
				System.out.format("%d",field[i][j]);
			}
			System.out.println("");
		}
		System.out.println("");
	}

	private void pause()
	{
		if (paused)
		{
			timer.start();
			updateScore(true);
		}
		else
		{
			timer.stop();
			ScoreLabel.setText("Pauza");
		}
		paused^=true;
	}

	private void end()
	{
		updateScore(false);
		timer.stop();
		System.exit(0);
	}

	@Override
	public void actionPerformed(ActionEvent a)
	{
		step();
	}

	class FieldPanel extends JPanel
	{
		private int Height, Width;

		public FieldPanel(int height, int width)
		{
			setPreferredSize(new Dimension(width*30-2,height*30));
			Height=height;
			Width=width;
		}

		private void doDrawing(Graphics g)
		{
			Graphics2D g2d =(Graphics2D) g;
			for (int i=1, x=29; i<Width; i++, x+=30)
			{
				g2d.drawLine(x,0,x,Height*30);
			}
			for (int i=0, x=29; i<=Height; i++, x+=30)
			{
				g2d.drawLine(0,x,Width*30,x);
			}

			Color c =new Color(255,255,255);

			for (int i=0; i<Height; i++)
			{
				for (int j=0; j<Width; j++)
				{
					switch(field[i][j])
					{
						case EMPTY:		c =new Color(255,255,255);	break;
						case I_SHAPE: 	c =new Color(102,204,0);	break;
						case T_SHAPE: 	c =new Color(255,255,0);	break;
						case O_SHAPE:	c =new Color(255,153,255);	break;
						case L_SHAPE:	c =new Color(0,0,0);		break;
						case J_SHAPE:  	c =new Color(255,204,0);	break;
						case S_SHAPE: 	c =new Color(204,204,204);	break;
						case Z_SHAPE: 	c =new Color(102,102,255);	break;
						default:		c =new Color(255,255,255);	break;
					}

					g2d.setColor(c);
					g2d.fillRect(j*30,i*30,29,29);
				}
			}
		}

		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			doDrawing(g);
		}
	}

	class TetrisAdapter extends KeyAdapter
	{
		@Override
		public void keyPressed(KeyEvent k)
		{
			int code = k.getKeyCode();

			switch(code)
			{
				case KeyEvent.VK_LEFT: 	moveLeft(); 								break;
				case KeyEvent.VK_RIGHT: moveRight(); 								break;
				case KeyEvent.VK_UP: 	rotate(); 									break;
				case KeyEvent.VK_DOWN: 	timer.restart(); 	let_fall(); 			break;
				case KeyEvent.VK_SPACE: timer.restart(); 	lineDown(); repaint(); 	break;
				default: 															break;
			}

			if (code=='P'||code=='p')
			{
				pause();
				return;
			}
		}
	}
}

class MyDialog extends JFrame implements ActionListener
{
	private JLabel HeightLabel, WidthLabel, SpeedLabel;
	private JTextField HeightField, WidthField, SpeedField;
	private JButton CreateButton, ExitButton;

	MyDialog(String nazwa)
	{
		super(nazwa);
		setLocationRelativeTo(null);
		setSize(300,200);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new GridLayout(4,2));

		HeightLabel =new JLabel("Wysokość (10-25):",JLabel.CENTER);
		WidthLabel =new JLabel("Szerokość (5-15)",JLabel.CENTER);
		SpeedLabel =new JLabel("Prędkość (0-9):",JLabel.CENTER);

		HeightField =new JTextField("20");
		WidthField =new JTextField("10");
		SpeedField =new JTextField("0");

		CreateButton =new JButton("OK");
		CreateButton.addActionListener(this);
		ExitButton =new JButton("Wyjście");
		ExitButton.addActionListener(this);

		add(HeightLabel);  add(HeightField);
		add(WidthLabel);   add(WidthField);
		add(SpeedLabel);   add(SpeedField);
		add(CreateButton); add(ExitButton);
	}

	private void nope()
	{
		HeightField.setText("nope");
		WidthField.setText("nope");
		SpeedField.setText("nope");
	}

	@Override
	public void actionPerformed(ActionEvent a)
	{
		if (a.getSource()==CreateButton) 
		{
			try{
				createBoard(Integer.parseInt(HeightField.getText()),Integer.parseInt(WidthField.getText()),Integer.parseInt(SpeedField.getText()));
			}
			catch (Exception xD)
			{
				nope();
			}
		}
		else System.exit(0);
	}

	public void createBoard(int height, int width, int speed)
	{
		if (height<10 || height>25 || width<5 || width>15 || speed<0 || speed>9)
		{
			nope();
		}
		else
		{
			Board board =new Board(height,width,speed);
			board.setVisible(true);
			setVisible(false);
		}
	}
}

public class Tetris
{
	public static void main(String[] args) {
		MyDialog constructor =new MyDialog("Wybierz parametry");
		constructor.setVisible(true);
	}
}