package org.display;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Scanner;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import org.apache.log4j.Logger;
import org.com.DbMgr;
import org.com.dao.SimulationDAO;
import org.com.model.SchedulingAlgorithmBean;
import org.com.model.SimulationBean;
import org.conf.parameters.ReturnCodes;
import org.display.generation.MissionFileGeneratorMenu;
import org.display.system.JTerminal;
import org.graphstream.ui.swingViewer.View;
import org.positioning.LaserSystem;
import org.scheduling.MissionScheduler;
import org.scheduling.display.JMissionScheduler;
import org.scheduling.onlineACO.Ant;
import org.system.Terminal;
import org.time.TimeController;
import org.time.TimeScheduler;
import org.util.SimulationParameter;
import org.util.building.SimulationLoader;
import org.util.dbLoading.SimulationBuilder;

//import org.pushingpixels.substance.api.SubstanceLookAndFeel;

public class MainFrame {
	public static final String MAINFRAME_ICON_URL = "/etc/images/logo.jpeg";
	private static final Logger log = Logger.getLogger(MainFrame.class);

	private StateMgr stateManager;

	private static MainFrame instance;
	public static final String baseDirectory = "xml";
	private JFrame frame;
	private JMenuBar menuBar;
	private JMenu menuFile;
	private JMenu menuOptions;
	private JMenu menuControl;
	private JMenuItem stepSize, newSimulation, load, loadLast, /*replay,*/ scenarioGenerator, missionsGenerator, lhEventsGenerator;
	private JMenuItem jmiHiddenPlayPause, jmiNextStep, jmiStop;
	private JMenuItem jmiImportXML;


	private Toolbar toolbar;

	private JSplitPane splitPane;
	// private SimulationRunner op;
	public static final String skinNAME = "org.pushingpixels.substance.api.skin.MistSilverSkin";

	// public static final SubstanceLookAndFeel skin = new
	// SubstanceMistSilverLookAndFeel();

	private Integer currentSimID = null;

	private String localhostName;
	//private String lastOpen = "";
	//	private JDialog jdClosing;
	private View view;
	private JMissionScheduler jMissionScheduler;

	//private Component savedContentPane;
	// TOTO
	public static final int WIDTH = 1180;
	public static final int HEIGHT = 720;

	public static MainFrame getInstance() {
		if (instance == null)
			instance = new MainFrame();
		return instance;
	}

	private MainFrame() {

		scenarioGenerator = new JMenuItem("Scenario generator");
		scenarioGenerator.setMnemonic('s');
		scenarioGenerator.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		scenarioGenerator.setFont(GraphicDisplay.font);
		scenarioGenerator.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					public void run() {
						new ScenarioGeneratorMenu(MainFrame.this);
					}
				}.start();
			}
		});

		try {
			Runnable r = new Runnable() {

				@Override
				public void run() {
					//Renomme le thread Swing
					Thread.currentThread().setName("D2ctsGuiThread");

					frame = new JFrame("D²CTS");

					frame.setFont(GraphicDisplay.font);
					frame.getRootPane().setFont(GraphicDisplay.font);


					toolbar = new Toolbar(MainFrame.this);
					frame.add(toolbar, BorderLayout.PAGE_START);



					menuBar = new JMenuBar();
					menuBar.setOpaque(true);

					MouseListener focusMouseListener = new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent e) {
							setFocusOnJTerminal();
						}
					};
					MouseMotionListener focusMouseMotionListener = new MouseMotionAdapter() {
						@Override
						public void mouseMoved(MouseEvent e) {
							setFocusOnJTerminal();
						}
					};

					menuFile = new JMenu("File");
					menuFile.setMnemonic('f');
					menuFile.setFont(GraphicDisplay.font);
					menuFile.addMouseListener(focusMouseListener);
					menuFile.addMouseMotionListener(focusMouseMotionListener);

					newSimulation = new JMenuItem("New");
					newSimulation.setMnemonic('n');
					newSimulation.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
					newSimulation.setFont(GraphicDisplay.font);

					load = new JMenuItem("Load");
					load.setMnemonic('o');
					load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
					load.setFont(GraphicDisplay.font);

					jmiImportXML = new JMenuItem("Import XML");
					jmiImportXML.setMnemonic('i');
					jmiImportXML.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK));
					jmiImportXML.setFont(GraphicDisplay.font);

					menuControl = new JMenu("Control");
					menuControl.setFont(GraphicDisplay.font);
					menuControl.setMnemonic('c');
					menuControl.addMouseListener(focusMouseListener);
					menuControl.addMouseMotionListener(focusMouseMotionListener);

					jmiHiddenPlayPause = new JMenuItem("Play / Pause");
					jmiHiddenPlayPause.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
					jmiHiddenPlayPause.setFont(GraphicDisplay.font);
					jmiHiddenPlayPause.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							toolbar.playPause();
						}
					});

					jmiNextStep = new JMenuItem("Next Step");
					jmiNextStep.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
					jmiNextStep.setFont(GraphicDisplay.font);
					jmiNextStep.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							//if (jmiStop.isEnabled()) {
							toolbar.makeStep();

							//} else
							//	toolbar.makeFirstStep();
							splitPane.getTopComponent().requestFocus();

						}
					});

					jmiStop = new JMenuItem("Stop");
					jmiStop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
					jmiStop.setFont(GraphicDisplay.font);
					jmiStop.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							try {
								toolbar.stop().join();
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					});

					jmiHiddenPlayPause.setEnabled(false);
					jmiNextStep.setEnabled(false);
					jmiStop.setEnabled(false);

					menuControl.add(jmiHiddenPlayPause);
					menuControl.add(jmiNextStep);
					menuControl.add(jmiStop);

					loadLast = new JMenuItem("Load last");
					loadLast.setEnabled(false);

					loadLast.setMnemonic('l');
					loadLast.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
					loadLast.setFont(GraphicDisplay.font);

					/*replay = new JMenuItem("replay simulation");
					replay.setMnemonic('r');
					replay.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
					replay.setFont(GraphicDisplay.font);
					 */

					missionsGenerator = new JMenuItem("Missions generator");
					missionsGenerator.setMnemonic('m');
					missionsGenerator.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK));
					missionsGenerator.setFont(GraphicDisplay.font);
					missionsGenerator.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							load.setEnabled(false);
							loadLast.setEnabled(false);
							//						replay.setEnabled(false);
							scenarioGenerator.setEnabled(false);
							missionsGenerator.setEnabled(false);
							lhEventsGenerator.setEnabled(false);
							new MissionFileGeneratorMenu(MainFrame.this);
							load.setEnabled(true);
							loadLast.setEnabled(true);
							//						replay.setEnabled(true);
							scenarioGenerator.setEnabled(true);
							missionsGenerator.setEnabled(true);
							lhEventsGenerator.setEnabled(true);
						}
					});

					lhEventsGenerator = new JMenuItem("Laser head range events generator");
					lhEventsGenerator.setMnemonic('l');
					lhEventsGenerator.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK));
					lhEventsGenerator.setFont(GraphicDisplay.font);
					lhEventsGenerator.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							load.setEnabled(false);
							loadLast.setEnabled(false);
							//						replay.setEnabled(false);
							scenarioGenerator.setEnabled(false);
							missionsGenerator.setEnabled(false);
							lhEventsGenerator.setEnabled(false);
							new LHFileGeneratorMenu(MainFrame.this);
							load.setEnabled(true);
							loadLast.setEnabled(true);
							//						replay.setEnabled(true);
							scenarioGenerator.setEnabled(true);
							missionsGenerator.setEnabled(true);
							lhEventsGenerator.setEnabled(true);
						}
					});

					/*replay.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							load.setEnabled(false);
							loadLast.setEnabled(false);
							replay.setEnabled(false);
							initialStateGenerator.setEnabled(false);
							missionsGenerator.setEnabled(false);
							lhEventsGenerator.setEnabled(false);
							new StoredSimulationChooser(MainFrame.this);
						}
					});*/

					loadLast.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							loadLast();
						}
					});

					JMenuItem exit = new JMenuItem("exit");
					exit.setMnemonic('e');
					exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
					exit.setFont(GraphicDisplay.font);
					exit.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							exit();
						}
					});

					newSimulation.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							newSimulation();
						}
					});

					load.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							loadSimulation();
						}
					});

					jmiImportXML.addActionListener(new ActionListener(){
						@Override
						public void actionPerformed(ActionEvent e) {
							new ImportXMLGUI();
						}
					});

					menuFile.add(newSimulation);
					menuFile.add(load);
					menuFile.add(loadLast);
					menuFile.addSeparator();
					//menuFile.add(replay);
					//menuFile.addSeparator();
					menuFile.add(scenarioGenerator);
					menuFile.add(missionsGenerator);
					menuFile.add(lhEventsGenerator);
					menuFile.addSeparator();
					menuFile.add(jmiImportXML);
					menuFile.addSeparator();
					menuFile.add(exit);
					menuBar.add(menuFile);

					menuOptions = new JMenu("Options");
					menuOptions.setMnemonic('o');
					menuOptions.setFont(GraphicDisplay.font);
					menuOptions.addMouseListener(focusMouseListener);
					menuOptions.addMouseMotionListener(focusMouseMotionListener);

					stepSize = new JMenuItem("Set step size");
					stepSize.setMnemonic('s');
					stepSize.setFont(GraphicDisplay.font);
					stepSize.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
					stepSize.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							new StepSizeChooser(MainFrame.this);
						}
					});
					stepSize.setEnabled(false);

					menuOptions.add(stepSize);
					menuBar.add(menuOptions);

					menuBar.add(menuControl);

					frame.setJMenuBar(menuBar);

					Dimension d = new Dimension(WIDTH, HEIGHT);
					frame.setSize(d);
					frame.setMinimumSize(d);
					frame.setMaximumSize(d);
					frame.setPreferredSize(d);

					//GlassPane
					JPanel glassPane = new JPanel();
					glassPane.setBackground(new Color(50,50,50,128));
					glassPane.setSize(frame.getSize());
					frame.getRootPane().setGlassPane(glassPane);

					Point p = new Point(0, 0);
					frame.setLocation(p);

					frame.setIconImage(new ImageIcon(this.getClass().getResource(MAINFRAME_ICON_URL), "logo").getImage());

					splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);

					frame.getContentPane().setBackground(new Color(255, 255, 255, 0));
					frame.getContentPane().add(splitPane, BorderLayout.CENTER);

					frame.addWindowListener(new WindowAdapter() {
						@Override
						public void windowClosing(WindowEvent e) {
							exit();
						}
					});

					SwingUtilities.updateComponentTreeUI(frame);
					frame.setVisible(true);
				}

			};
			if(SwingUtilities.isEventDispatchThread()){
				Thread t = new Thread(r);
				t.start();
				t.join();
			} else {
				SwingUtilities.invokeAndWait(r);
			}
		} catch (InvocationTargetException | InterruptedException e1) {
			e1.printStackTrace();
		}

		stateManager = new StateMgr(stepSize, newSimulation, load, loadLast, /*replay,*/ scenarioGenerator, missionsGenerator, lhEventsGenerator,jmiHiddenPlayPause, jmiNextStep, jmiStop,jmiImportXML);

	}

	/*
	 * public static final String getSkin(){ return skin; }
	 */

	public JFrame getFrame() {
		return frame;
	}

	/*public void setSimReady(boolean ready) {
		if (ready) {
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));


		}
	}*/

	/*public Toolbar getToolbar() {
		return toolbar;
	}*/

	public void exit(){
		new Thread(){
			public void run () {
				try {
					toolbar.stop().join();
				} catch (InterruptedException ie) {
					log.fatal(ie.getMessage(), ie);
				}
				System.exit(ReturnCodes.EXIT_ON_SUCCESS.getCode());
			}
		}.start();
	}

	public void setWaitMode(final boolean on){
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					frame.getRootPane().getGlassPane().setVisible(on);
					if(jMissionScheduler!=null)
						jMissionScheduler.setWaitMode(on);
					if(on){
						frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					} else {
						frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					}
					SwingUtilities.updateComponentTreeUI(frame);		
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	public void loadSimulation(final Integer simID) {
		if(currentSimID != null){
			try {
				closeSimulation().join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		currentSimID = simID;

		SimulationBean bean = SimulationDAO.getInstance().get(simID);
		Terminal.getInstance().setTextDisplay(GraphicDisplayPanel.getInstance());
		SimulationLoader.getInstance().load(bean);
	}

	/*
	 * public void openReplaySimulation(final String configFile, final String
	 * simID){ frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	 * op = new
	 * SimulationRunner(configFile,MainFrame.this,localhostName,simID,true);
	 * jmiHiddenPlayPause.setEnabled(true); jmiNextStep.setEnabled(true);
	 * 
	 * }
	 */

	public void setGraphicDisplay(final JPanel panel) {
		//		SwingUtilities.invokeLater(new Runnable() {
		//			@Override
		//			public void run() {
		splitPane.setBottomComponent(panel);
		splitPane.setUI(new mySplitPaneUI());
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(0.7);
		//			}
		//		});
	}

	public void setJTerminal(final View panel) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view = panel;
				splitPane.setTopComponent(panel);
				splitPane.setDividerLocation(0.7);
				SwingUtilities.updateComponentTreeUI(frame);				
			}
		});
	}

	private class mySplitPaneUI extends BasicSplitPaneUI {
		public mySplitPaneUI() {
			super();
			this.dividerSize = 10;
			this.setNonContinuousLayoutDivider(new JPanel(), true);
		}
	}

	public JMenuItem getStepSizeMenuItem() {
		return stepSize;
	}

	public Thread closeSimulation() {
		//		Thread t = new Thread() {
		//			public void run() {
		//				Thread.currentThread().setName("ClosingThread");
		//				stepSize.setEnabled(false);
		//				jdClosing = new JDialog(frame, "Closing...", false);
		//				jdClosing.add(new JLabel("Closing simulation...", JLabel.CENTER), BorderLayout.CENTER);
		//				jdClosing.setSize(300, 75);
		//				frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		//				jdClosing.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		//				jdClosing.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		//				jdClosing.setLocation(frame.getLocation().x + (frame.getSize().width / 2 - jdClosing.getSize().width / 2), frame.getLocation().y
		//						+ (frame.getSize().height / 2 - jdClosing.getSize().height / 2));
		//				jdClosing.setAlwaysOnTop(true);
		//				jdClosing.setVisible(true);
		//			}
		//		};
		//		SwingUtilities.invokeLater(t);
		//		try {
		//			t.join();
		//		} catch (InterruptedException e1) {
		//			e1.printStackTrace();
		//		}

		currentSimID = null;
		Thread t = new Thread(){
			public void run(){
				setWaitMode(true);
				load.setEnabled(false);
				loadLast.setEnabled(false);
				//							replay.setEnabled(false);
				scenarioGenerator.setEnabled(false);
				missionsGenerator.setEnabled(false);
				lhEventsGenerator.setEnabled(false);
				stepSize.setEnabled(false);
				//					try{
				//						SwingUtilities.invokeAndWait(new Runnable(){
				//							public void run(){
				//								
				//								
				//							}
				//						});
				//					} catch (Exception e){
				//						log.error(e);
				//					}

				try {
					DbMgr.getInstance().commitAndClose();
					Terminal.closeInstance();
					MissionScheduler.closeInstance();
					LaserSystem.closeInstance();
					JTerminal.closeInstance();
					TimeScheduler.closeInstance();
					GraphicDisplayPanel.closeInstance();
					SimulationLoader.closeInstance();
					Ant.resetAll();
					toolbar.reset();
					setJTerminal(null);
					removeSplitPaneContent();
					//	SwingUtilities.updateComponentTreeUI(MainFrame.this.frame);
				} catch (SQLException e) {
					log.fatal(e.getMessage(), e);
				}
				//
				//				closeJDClosing();
				log.info("---------------------------------------------------------------");
				//					try{
				//						SwingUtilities.invokeAndWait(new Runnable(){
				//							public void run(){
				//								/*load.setEnabled(true);
				//							loadLast.setEnabled(true);
				//							//							replay.setEnabled(true);
				//							initialStateGenerator.setEnabled(true);
				//							missionsGenerator.setEnabled(true);
				//							lhEventsGenerator.setEnabled(true);
				//
				//							jmiHiddenPlayPause.setEnabled(false);
				//							jmiNextStep.setEnabled(false);
				//							jmiStop.setEnabled(false);
				//								 */
				//								
				//							}
				//						});
				//					} catch (Exception e){
				//						log.error(e);
				//					}
				stateManager.load();
				setWaitMode(false);
			}
		};

		t.setName("ClosingThread");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
		return t;

	}

	public String getLocalHostName() {
		return localhostName;
	}

	/*public void closeJDClosing() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				jdClosing.setVisible(false);
				frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				jdClosing.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				jdClosing.dispose();
			}
		});
	}*/

	public void removeSplitPaneContent() {
		splitPane.removeAll();
		splitPane.validate();
		splitPane.updateUI();
	}

	//	public void enableMenus() {
	//		load.setEnabled(true);
	//		loadLast.setEnabled(true);
	//		//		replay.setEnabled(true);
	//		initialStateGenerator.setEnabled(true);
	//		missionsGenerator.setEnabled(true);
	//		lhEventsGenerator.setEnabled(true);
	//		stateManager.save();
	//	}

	//	public void enableStopMenuItem() {
	//		jmiStop.setEnabled(true);
	//		stateManager.save();
	//	}

	public void setFocusOnJTerminal() {
		if (view != null) {
			view.requestFocus();
		}
	}

	/*public void setEnableMenus(boolean state) {
		menuFile.setEnabled(state);
		menuOptions.setEnabled(state);
		menuControl.setEnabled(state);
		stateManager.save();
	}*/

	public void setSimReady() {

		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					setGraphicDisplay(GraphicDisplayPanel.getInstance().getPanel());
					toolbar.setTimeControler(new TimeController());
					stateManager.save();
					jmiNextStep.setEnabled(true);
					jmiStop.setEnabled(true);
					jmiHiddenPlayPause.setEnabled(true);
					setJTerminal(JTerminal.getInstance().getPanel());
					toolbar.activate();
					stepSize.setEnabled(true);

				}
			});
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setWaitMode(false);
	}

	private void loadLast() {
		Thread t = new Thread(){
			public void run(){
				Thread.currentThread().setName("D2ctsLoaderThread");
				setWaitMode(true);
				stateManager.save();
				load.setEnabled(false);
				loadLast.setEnabled(false);
				//							replay.setEnabled(false);
				scenarioGenerator.setEnabled(false);
				missionsGenerator.setEnabled(false);
				lhEventsGenerator.setEnabled(false);

				//				try{
				//					SwingUtilities.invokeAndWait(new Runnable(){
				//						public void run(){
				//							
				//					
				//						}
				//					});
				//				} catch (Exception e){
				//					log.error(e);
				//				}


				loadSimulation(getLastOpen());

				//				try{
				//					SwingUtilities.invokeAndWait(new Runnable(){
				//						public void run(){
				//
				//							
				//						}
				//					});
				//				} catch (Exception e){
				//					log.error(e);
				//				}
				stateManager.load();
				//							jmiNextStep.setEnabled(true);
				//							jmiHiddenPlayPause.setEnabled(true);
				//							stepSize.setEnabled(true);

				//setWaitMode(false);
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();

	}




	private void loadSimulation(){
		Thread t = new Thread(){
			public void run(){
				setWaitMode(true);
				stateManager.save();

				//				try{
				//					SwingUtilities.invokeAndWait(new Runnable(){
				//						public void run(){
				//							
				//							//load.setEnabled(false);
				//							//loadLast.setEnabled(false);
				//							//							replay.setEnabled(false);
				//							//initialStateGenerator.setEnabled(false);
				//							//missionsGenerator.setEnabled(false);
				//							//lhEventsGenerator.setEnabled(false);
				//						}
				//					});
				//				} catch(Exception e){
				//					log.error(e);
				//				}
				// Set simulation chooser panel as in New Simulation
				// SIM ID - DATE REC - SCENARIO ID - SCENARIO NAME -
				// TERMINAL ID - TERMINAL LABEL
				final LoadSimulationDialog simDialog = new LoadSimulationDialog(frame);
				// Lock call
				final Integer simID = simDialog.getSelection();

				if (simID != null) {
					setLastOpen(simID);
					// Open the given simulation
					Thread.currentThread().setName("D2ctsLoaderThread");

					loadSimulation(simID);
				}

				//				try{
				//					SwingUtilities.invokeAndWait(new Runnable(){
				//						public void run(){
				//							
				//							
				//						}
				//					});
				//				} catch(Exception e){
				//					log.error(e);
				//				}
				if(simID != null){
					//								jmiNextStep.setEnabled(true);
					//								jmiHiddenPlayPause.setEnabled(true);
					//								
					//					toolbar.activate();

				} else {
					stateManager.load();
					setWaitMode(false);
				}

			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();

	}

	private void newSimulation(){
		Thread t = new Thread(){
			public void run(){
				setWaitMode(true);
				/*try{
					SwingUtilities.invokeAndWait(new Runnable(){
						public void run(){

							//							load.setEnabled(false);
							//							loadLast.setEnabled(false);
							//							replay.setEnabled(false);
							//							initialStateGenerator.setEnabled(false);
							//							missionsGenerator.setEnabled(false);
							//							lhEventsGenerator.setEnabled(false);
						}
					});
				} catch(Exception e){
					log.error(e);
				}*/

				final NewSimulationDialog simDialog = new NewSimulationDialog(frame);
				// Lock call
				SimulationParameter parameters = simDialog.getSelection();
				if(parameters != null){
					Integer scenarioID = parameters.getScenarioID();

					SchedulingAlgorithmBean schedulingAlgorithm = parameters.getSchedulingAlgorithmBean();
					if (scenarioID != null && schedulingAlgorithm != null) {
						// Create a simulation according to the given
						// scenario properties

						SimulationBuilder builder = new SimulationBuilder(scenarioID, schedulingAlgorithm, parameters.getSeed());
						builder.build();
						final SimulationBean bean = builder.getSimulationBean();
						if (bean != null) {
							setLastOpen(bean.getId());
							Terminal.getInstance().setTextDisplay(GraphicDisplayPanel.getInstance());
							currentSimID = bean.getId();
							SimulationLoader.getInstance().load(bean);

							//							try{
							//								SwingUtilities.invokeAndWait(new Runnable(){
							//									public void run(){
							//										//										jmiNextStep.setEnabled(true);
							//										//										jmiHiddenPlayPause.setEnabled(true);
							//										//										stepSize.setEnabled(true);
							//							
							//									
							//									}
							//								});
							//							} catch(Exception e){
							//								log.error(e);
							//							}
							//							toolbar.activate();
							//							setWaitMode(false);
						}
					}
				}
			}
		};
		t.setName("D2ctsLoaderThread");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	private Integer getLastOpen(){
		Integer simID = null;
		try {
			File f = new File(this.getClass().getResource("../../conf/lastOpenSimulation.dat").getPath());
			Scanner scan = new Scanner(f);
			String lastOpen = scan.nextLine();
			if(lastOpen!=null && !lastOpen.equals("")){
				simID = Integer.parseInt(lastOpen);
				//if(lastOpen...
				//TODO loadLast.setEnabled(true);
			}
			scan.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return simID;

	}

	private void setLastOpen(final Integer id) {
		File f = new File(this.getClass().getResource("../../conf/lastOpenSimulation.dat").getPath());
		if(f.exists()){
			f.delete();
		}

		PrintWriter pwLast = null;
		try {
			f.createNewFile();
			pwLast = new PrintWriter(f);
			pwLast.append(id+"");
			pwLast.flush();
			loadLast.setEnabled(id!=null);
			//			SwingUtilities.invokeLater(new Runnable(){
			//				public void run() {
			//				
			//				}
			//			});
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(pwLast != null){
				pwLast.close();
			}
		}
	}

	public void setJMissionScheduler(JMissionScheduler jMissionScheduler) {
		this.jMissionScheduler = jMissionScheduler;

	}
}