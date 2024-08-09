import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Clip;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.JFormattedTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

public class PercussionSequencer {
    private JFrame frame;
    private JButton[][] buttons;
    private final Color cyan = new Color(0, 188, 212);
    private final Color yellow = new Color(255, 193, 0);
    private final Color red = new Color(246, 63, 44);
    private final Color blue = new Color(0, 166, 247);
    private final Color green = new Color(70, 176, 74);
    private final Color purple = new Color(102, 52, 156);
    private final Color pink = new Color(235, 22, 96);
    private final Color orange = new Color(255, 115, 28);
    private final Color lightGray = new Color(225, 225, 225);
    private final Color lightBlue = new Color(237, 248, 254);
    private final Color lightBlueDark = new Color(228, 239, 245);
    private final Color white = new Color(255, 255, 255);
    private final Color[] colors = {cyan, yellow, red, blue, green, purple, pink, orange};
    private static Clip clip;
    private boolean spacebarPressed = false;
    private Thread loopThread; // To handle the indefinite loop in a separate thread
    private File selectedFile;
    private int tempo = 140;
    private JSlider tempoSlider;
   
    public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					PercussionSequencer window = new PercussionSequencer();
					playSound("sfx//startup.wav");
					window.frame.setVisible(true);
					window.frame.setResizable(false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
    }

    public PercussionSequencer(){
        initialize();
    }
    /**
     * @wbp.parser.entryPoint
     */
    private void initialize() {
    	FlatMacLightLaf.setup();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // Calculate the center coordinates
        int centerX = (screenSize.width - 1200) / 2;
        int centerY = (screenSize.height - 600) / 2;
        
        frame = new JFrame();
        frame.setBounds(centerX, centerY, 1200, 600);
        //frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        
        JMenuBar menuBar = new JMenuBar();
     // ----------------------------------------------------------------------- file menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAsItem = new JMenuItem("Save As");
        
        openItem.addActionListener(e -> open());
        saveItem.addActionListener(e -> save());
        saveAsItem.addActionListener(e -> saveAs());
        
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
// ----------------------------------------------------------------------- option menu
        JMenu optionMenu = new JMenu("Options");
        JMenuItem resetItem = new JMenuItem("Reset");
        JMenuItem exitItem = new JMenuItem("Exit");
        
        resetItem.addActionListener(e -> reset());
        exitItem.addActionListener(e -> System.exit(0));
        
        optionMenu.add(resetItem);
        optionMenu.addSeparator();
        optionMenu.add(exitItem);
     // ------------------------------------------------------------------ presets menu
        JMenu presetsMenu = new JMenu("Presets");
        JMenuItem example1 = new JMenuItem("Example 1");
        JMenuItem example2 = new JMenuItem("Example 2");
        JMenuItem example3 = new JMenuItem("Example 3");
        JMenuItem example4 = new JMenuItem("Example 4");
        JMenuItem example5 = new JMenuItem("Example 5");
        
        JMenuItem[] examples = {example1, example2, example3, example4, example5};
        for(JMenuItem example : examples) {
            presetsMenu.add(example);
            example.addActionListener(e -> example(example.getText().toString()));
        }
        
        // ------------------------------------------------------------------ help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem manual = new JMenuItem("Manual");
        JMenuItem about = new JMenuItem("About");
        JMenuItem shortcuts = new JMenuItem("Keyboard Shortcuts");
        
        JMenuItem[] helpItems = {manual, about, shortcuts};
        
        for(JMenuItem helpItem : helpItems) {
            helpMenu.add(helpItem);
            helpItem.addActionListener(e -> displayHelpItem(helpItem.getText().toString()));
        }
        
        JMenu[] menus = {fileMenu, optionMenu, presetsMenu, helpMenu};
        
        for(JMenu menu : menus) {
        	menuBar.add(menu);
        }
        
        frame.setJMenuBar(menuBar);

        JPanel gridPanel = new JPanel(new GridLayout(8, 16, 1, 1)); // Add 1-pixel gap between buttons
        gridPanel.setBounds(240, 120, 706, 323);
        buttons = new JButton[8][16];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 16; j++) {
                buttons[i][j] = new JButton();
                buttons[i][j].setOpaque(true);
                buttons[i][j].setFocusable(false);
                buttons[i][j].setBorderPainted(false);
                final int row = i; // annoyingly they have to be effectivly final 
                final int col = j; // for the lambda expression to work vvvvvv
                if((col>3 && col<8)||(col>11)) {
                    buttons[i][j].setBackground(lightGray);
                } else {
                    buttons[i][j].setBackground(white);
                }
                buttons[i][j].addActionListener(e -> toggleButton(row, col));
                gridPanel.add(buttons[i][j]);
            }
        }
        //frame.getContentPane().add(gridPanel, BorderLayout.CENTER);

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    spacebarPressed = !spacebarPressed; // Toggle state

                    if (spacebarPressed) {
                        // Spacebar pressed for the first time, start the loop
                        if (loopThread == null || !loopThread.isAlive()) {
                            loopThread = new Thread(() -> {
                                while (spacebarPressed) {
                                	play(0, 15);
                                }
                            });
                            loopThread.start();
                        }
                    } else {
                    	clip.stop();
                    }}
                else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
                	save();
                }
                else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_O) {
                	open();
                }
                else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Q) {
                	System.exit(0);
                }
                else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_R) {
                	reset();
                }
            }
        });    
        JPanel containerPanel = new JPanel();
        //containerPanel.setBorder(BorderFactory.createEmptyBorder(frame.getWidth()/10, frame.getWidth()/5, frame.getWidth()/10, frame.getWidth()/5)); // Add padding around the grid panel
        containerPanel.setLayout(null);

        containerPanel.add(gridPanel); // Add the grid panel to the center of the container panel
        frame.getContentPane().add(containerPanel, BorderLayout.CENTER);
        
        JFormattedTextField tempoField = new JFormattedTextField();
        tempoField.setEditable(false);
        tempoField.setHorizontalAlignment(SwingConstants.CENTER);
        tempoField.setFont(new Font("Tahoma", Font.PLAIN, 44));
        tempoField.setBounds(591, 450, 108, 44);
        tempoField.setFocusable(false);
        containerPanel.add(tempoField);
        tempoField.setValue(140); // Set initial value
        tempoField.setColumns(4); // Set the number of columns for proper sizing
        
                // Create the JSlider
        tempoSlider = new JSlider(JSlider.HORIZONTAL, 40, 240, 140);
        tempoSlider.setBounds(50, 496, 1086, 44);
        containerPanel.add(tempoSlider);
        tempoSlider.setMajorTickSpacing(25); // Major ticks every 25 units
        tempoSlider.setMinorTickSpacing(5); // Minor ticks every 5 units
        tempoSlider.setPaintTicks(true); // Show tick marks
        tempoSlider.setPaintLabels(true); // Show numerical labels
        tempoSlider.setFocusable(false);
        
        JLabel lblNewLabel = new JLabel("BPM:");
        lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 44));
        lblNewLabel.setBounds(476, 445, 119, 54);
        lblNewLabel.setFocusable(false);
        containerPanel.add(lblNewLabel);
        
                // Add a ChangeListener to the tempoSlider to update the tempoField
        tempoSlider.addChangeListener(e -> {
        	int selectedTempo = tempoSlider.getValue();
            tempoField.setValue(selectedTempo);
            tempo = selectedTempo;
        });
        
        JPanel sliderPanel = new JPanel();
        sliderPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50)); // Add padding
        sliderPanel.setLayout(null);
        sliderPanel.setFocusable(false);


        // Add the sliderPanel to the frame with padding
        frame.getContentPane().add(sliderPanel, BorderLayout.SOUTH);
        frame.requestFocusInWindow();
    }
    
    private void displayHelpItem(String item) {
    	switch(item) {
    	case "Manual":
        	JOptionPane.showMessageDialog(null, ""
        			+ "User Manual\n\n"
        			+ "This is a classic 808 drum machine\n\n"
        			+ "This is a percussion sequencer which is a simple way to make\n\n"
        			+ "beats. These small repeating patterns are called 'sequences'\n\n"
        			+ "You can save, open and edit these different sequences.\n\n"
        			+ "You can also change the speed in terms of tempo with a range of 40-240 of beats per minute\n\n"
        			+ "Cyan: Cymbal\n"
        			+ "Orange: Open Hi Hat\n"
        			+ "Red: Closed Hi Hat\n"
        			+ "Blue: Clap\n"
        			+ "Green: Cowbell\n"
        			+ "Purple: Snare\n"
        			+ "Pink: Low Tom\n"
        			+ "Orange: Kick", "User Manual + some info", JOptionPane.INFORMATION_MESSAGE);
    		break;
    		
    	case "About":
        	JOptionPane.showMessageDialog(null, ""
        			+ "Percussion Sequencer\n"
        			+ "Version 1.0\n\n"
        			+ "Description:\n"
        			+ "Percussion Sequencer is a Java-based application for creating and saving percussion sequences.\n\n"
        			+ "Developer:\n"
        			+ "Aaryan Sachdeva\n\n"
        			+ "Usage Rights:\n"
        			+ "Free use (no copyright)\n\n"
        			+ "Release Date:\n"
        			+ "March 10, 2024\n\n"
        			+ "Contact Information:\n"
        			+ "For support or feedback, please contact aaryansachdeva10@gmail.com."
        			, "About", JOptionPane.INFORMATION_MESSAGE);
    		break;
    		
    	case "Keyboard Shortcuts":
        	JOptionPane.showMessageDialog(null, ""
        			+ "Space = Play\n\n"
        			+ "Ctrl + S = Save\n\n"
        			+ "Ctrl + O = Open\n\n"
        			+ "Ctrl + R = Reset\n\n"
        			+ "Ctrl + Q = Quit"
        			, "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
    		break;
    	}
    }
    
    private void saveAs() {
    	if(!clip.isRunning()) {
	        // Create a JFileChooser instance
	        JFileChooser fileChooser = new JFileChooser();
	
	        // Optional: Set a default directory
	        fileChooser.setCurrentDirectory(new File("C:\\Users\\Manish\\eclipse-workspace\\Percussion-Sequencer\\sequences"));
	        FileNameExtensionFilter filter = new FileNameExtensionFilter("TXT Files", "txt");
	
	        fileChooser.setFileFilter(filter);
	 
	        // Show the Save As dialog
	        int userSelection = fileChooser.showSaveDialog(null);
	
	        if (userSelection == JFileChooser.APPROVE_OPTION) {
	            File fileToSave = fileChooser.getSelectedFile();
	
	            if (!fileToSave.getAbsolutePath().toLowerCase().endsWith(".txt")) {
	                fileToSave = new File(fileToSave.getAbsolutePath() + ".txt");
	            }
	            // Save content to the selected file
	            try (FileWriter fw = new FileWriter(fileToSave);
	                 BufferedWriter bw = new BufferedWriter(fw)) {
	            	
	     // actual writing algorithm (basically reading but instead backwards)
	                for(int x=0; x<=7; x++) {
	                	bw.write((x==0) ? "" : "\n");
	                	
	                    for(int y = 0; y <= 15; y++) {
	                    	Color bg = buttons[x][y].getBackground();
	                        bw.write(((bg == white) || (bg == lightGray) || (bg == lightBlueDark)) ? "0" : "1");
	                    }     // ternary operators are so cool ^^^^^^^^
	                }
	                bw.write("\n" + tempo);
	            	JOptionPane.showMessageDialog(null, "Saved at: " + fileToSave.getAbsolutePath(), "Saved Path", JOptionPane.INFORMATION_MESSAGE);
	        		
	                selectedFile = fileToSave.getAbsoluteFile();
	            } catch (IOException e) {
	                JOptionPane.showMessageDialog(null, "An error occurred while saving the file.", "Error", JOptionPane.ERROR_MESSAGE);
	                e.printStackTrace();
	            }
	         }
    	}
    	else {
    		JOptionPane.showMessageDialog(null, "Pause before saving!", "Error", JOptionPane.ERROR_MESSAGE);
    	}
    }
    
    private void open() {
    	reset();
        JFileChooser fileChooser = new JFileChooser();
        
        fileChooser.setCurrentDirectory(new File("C:\\Users\\Manish\\eclipse-workspace\\Percussion-Sequencer\\sequences"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("TXT Files", "txt");

        fileChooser.setFileFilter(filter);
        int result = fileChooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            //System.out.println("Selected file: " + selectedFile.getAbsolutePath());
        }
        try (Scanner scanner = new Scanner(new File(selectedFile.toString()))) {
            for(int x=0; x<=7; x++) {
                String line = scanner.nextLine();
                
                char[] charArray = line.toCharArray();
                //System.out.println(charArray[x]);
                //System.out.println(charArray[0]);

                
                for(int y = 0; y <= 15; y++) {
                    if(charArray[y] == '1' && x < colors.length) {
                        buttons[x][y].setBackground(colors[x]);
                    }
            	}
            }
            List<String> allLines = Files.readAllLines(Paths.get(selectedFile.getAbsolutePath()));
            tempo = Integer.parseInt(allLines.get(allLines.size() - 1));
            tempoSlider.setValue(tempo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void save() {
    	
    }
    
    private void reset() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 16; j++) {
               buttons[i][j].setBackground(((j>3 && j<8)||(j>11)) ? lightGray : white);
            }
        }
    }

    private void toggleButton(int row, int col) {
        if (buttons[row][col].getBackground().equals(white) || buttons[row][col].getBackground().equals(lightGray)) {
            // Set the color and draw the circle
        	String[] sounds = new String[]{
        		    "sfx//cymbal.wav",
        		    "sfx//open-hi-hat.wav",
        		    "sfx//closed-hi-hat.wav",
        		    "sfx//clap.wav",
        		    "sfx//cowbell.wav",
        		    "sfx//snare.wav",
        		    "sfx//low-tom.wav",
        		    "sfx//kick.wav"
        		};
        		// Apply color and play sound based on the row
        		if (row >= 0 && row < colors.length) {
        		    buttons[row][col].setBackground(colors[row]);
        		    playSound(sounds[row]);
        		}
        } else {
            buttons[row][col].setBackground(((col>3 && col<8)||(col>11)) ? lightGray : white);
        }
    }
    private void play(int startColumn, int endColumn) { 
        for (int column = startColumn; column <= endColumn; column++) {
            int prevColumn = (column == 0) ? buttons[0].length - 1 : column - 1;
            resetColumn(prevColumn);

            for (int row = 0; row < buttons.length; row++) {
                JButton currentButton = buttons[row][column];
                Color buttonColor = currentButton.getBackground();

                Color newColor = buttonColor.equals(white) ? lightBlue :
                                 buttonColor.equals(lightGray) ? lightBlueDark : buttonColor;

                currentButton.setBackground(newColor);
                returnSound(buttonColor); // Assuming this method plays a sound based on the button color
            }

            try {
                double t = 2400 / tempo - 5f;
                int milliSeconds = (int) Math.floor(t);
                double decimalPart = t - (int) t;
                int nanoSeconds = (int) Math.round(decimalPart * 1000f);

                Thread.sleep(milliSeconds * 8, nanoSeconds); // sleep delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void resetColumn(int column) {
        for (int row = 0; row < buttons.length; row++) {
            // Reset the color of each button in the column
        	if(buttons[row][column].getBackground() == lightBlue) {
                buttons[row][column].setBackground(white);
            }
        	else if(buttons[row][column].getBackground() == lightBlueDark) {
        		buttons[row][column].setBackground(lightGray);
        	}
        }
    }

    private void returnSound(Color color) {
    	//System.out.println(color.toString());
        switch (color.toString()) {
/*cyan*/    case "java.awt.Color[r=0,g=188,b=212]": playSound("sfx//cymbal.wav"); break;
            	
/*yellow*/  case "java.awt.Color[r=255,g=193,b=0]": playSound("sfx//open-hi-hat.wav"); break;
            	
/*red*/     case "java.awt.Color[r=246,g=63,b=44]": playSound("sfx//closed-hi-hat.wav"); break;
                
/*blue*/    case "java.awt.Color[r=0,g=166,b=247]": playSound("sfx//clap.wav"); break;
                
/*green*/   case "java.awt.Color[r=70,g=176,b=74]": playSound("sfx//cowbell.wav"); break;
            
/*purple*/  case "java.awt.Color[r=102,g=52,b=156]": playSound("sfx//snare.wav"); break;

/*pink*/    case "java.awt.Color[r=235,g=22,b=96]": playSound("sfx//low-tom.wav"); break;
            
/*orange*/  case "java.awt.Color[r=255,g=115,b=28]": playSound("sfx//kick.wav"); break;
        }
    }
    
    private void example(String example) {
    	reset();
    	
    	switch(example) {
	    	case "Example 1":
	    		for(int x=0; x<=7; x++) {
	    			buttons[2][2*x].setBackground(red);
	    		}
	    		buttons[7][0].setBackground(orange);
	    		buttons[7][8].setBackground(orange);
	    		buttons[5][12].setBackground(purple);
	    		buttons[5][4].setBackground(purple);
	    		break;
	    		
	    	case "Example 2":
	    		for(int x=0; x<=7; x++) {
	    			buttons[2][2*x].setBackground(red);
	    		}
	    		buttons[7][0].setBackground(orange);
	    		buttons[7][8].setBackground(orange);
	    		buttons[7][15].setBackground(orange);
	    		buttons[5][12].setBackground(purple);
	    		buttons[6][2].setBackground(pink);
	    		buttons[5][4].setBackground(purple);
	    		buttons[4][8].setBackground(green);
	    		break;
	    	
	    	case "Example 3":
	    		for(int x=0; x<=7; x++) {
	    			buttons[2][2*x].setBackground(red);
	    		}
	    		buttons[7][0].setBackground(orange);
	    		buttons[7][7].setBackground(orange);
	    		buttons[7][8].setBackground(orange);
	    		buttons[7][10].setBackground(orange);
	    		buttons[7][14].setBackground(orange);
	    		buttons[5][4].setBackground(purple);
	    		buttons[5][12].setBackground(purple);
	    		tempoSlider.setValue(130);
	    		break;
	    		
	    	case "Example 4":
	    		for(int x=0; x<=3; x++) {
	    			buttons[2][4*x].setBackground(red);
	    			buttons[7][4*x].setBackground(orange);
	    			buttons[4][4*x+2].setBackground(green);
	    		}
	    		break;
	    		
	    	case "Example 5":
	    		for(int x=0; x<=3; x++) {
	    			buttons[4][4*x].setBackground(green);
	    			buttons[7][4*x+2].setBackground(orange);
	    		}
	    		buttons[0][14].setBackground(cyan);
	    		buttons[3][2].setBackground(blue);
	    		buttons[6][6].setBackground(pink);
	    		buttons[6][14].setBackground(pink);
	    		for(int x=0; x<=2; x++){
	    			buttons[2][x+3].setBackground(red);
	    		}
	    		buttons[2][10].setBackground(red);
	    		buttons
	    		[2][8].setBackground(red);
	    		break;
	    	}
    }
        public static void playSound(String soundFilePath) {
        try {
            File soundFile = new File(soundFilePath);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat format = audioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(audioInputStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}