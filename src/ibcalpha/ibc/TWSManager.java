/* 
 * Copyright (c) 2020 James Leigh
 * 
 * This program is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU General Public License as published by  
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ibcalpha.ibc;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;

/**
 * Handle various actions to the TWS software
 * 
 * @author James Leigh
 *
 */
public class TWSManager {

	public synchronized static boolean isOpen() {
		JFrame jf = MainWindowManager.mainWindowManager().getMainWindow(0, TimeUnit.MILLISECONDS);
		return jf != null && jf.isDisplayable();
	}

	public synchronized static void start(Class<?> mainClass, String ibDir) throws IOException, ClassNotFoundException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		createToolkitListener();
		JtsIniManager.initialise(getJtsIniFilePath(ibDir));
		startTws(mainClass, ibDir);
	}

	public synchronized static void stop() {
		(new StopTask(null)).run(); // run on the current thread
	}

	public synchronized static int enableAPI(Integer portNumber, Boolean readOnly)
			throws InterruptedException, IbcException, ExecutionException {
		if (readOnly != null) {
			(new ConfigurationTask(new ConfigureReadOnlyApiTask(readOnly))).executeAsync();
		}

		final AtomicInteger currentPort = new AtomicInteger();
		final JDialog configDialog = ConfigDialogManager.configDialogManager().getConfigDialog();
		FutureTask<?> t = new FutureTask<>((Runnable) () -> {
			try {
				Utils.selectApiSettings(configDialog);

				Component comp = SwingUtils.findComponent(configDialog, "Socket port");
				if (comp == null)
					throw new IbcException("could not find socket port component");

				JTextField tf = SwingUtils.findTextField((Container) comp, 0);
				if (tf == null)
					throw new IbcException("could not find socket port field");

				currentPort.set(Integer.parseInt(tf.getText()));
				if (portNumber != null && currentPort.get() == portNumber) {
					Utils.logToConsole("TWS API socket port is already set to " + tf.getText());
				} else if (portNumber != null && portNumber > 0) {
					Utils.logToConsole("TWS API socket port was set to " + tf.getText());
					tf.setText(Integer.toString(portNumber));
					Utils.logToConsole("TWS API socket port now set to " + tf.getText());
				}
				if (!MainWindowManager.mainWindowManager().isGateway()) {
					JCheckBox cb = SwingUtils.findCheckBox(configDialog, "Enable ActiveX and Socket Clients");
					if (cb == null)
						throw new IbcException("could not find Enable ActiveX checkbox");
					if (cb.isSelected())
						ConfigDialogManager.configDialogManager().setApiConfigChangeConfirmationExpected();

					if (!cb.isSelected()) {
						cb.doClick();
						SwingUtils.clickButton(configDialog, "OK");
					}
				}
			} catch (IbcException e) {
				throw new UndeclaredThrowableException(e);
			}
		}, null);
		GuiExecutor.instance().execute(t);
		try {
			t.get();
		} catch (ExecutionException e) {
			try {
				try {
					throw e.getCause();
				} catch (UndeclaredThrowableException cause) {
					throw cause.getCause();
				}
			} catch (RuntimeException | InterruptedException | IbcException cause) {
				throw cause;
			} catch (Throwable cause) {
				throw e;
			}
		} finally {
			ConfigDialogManager.configDialogManager().releaseConfigDialog();
		}
		return currentPort.get();
	}

	public static synchronized void saveSettings() {
		Utils.invokeMenuItem(MainWindowManager.mainWindowManager().getMainWindow(),
				new String[] { "File", "Save Settings" });
	}

	public synchronized static void reconnectData() {
		JFrame jf = MainWindowManager.mainWindowManager().getMainWindow(1, TimeUnit.MILLISECONDS);

		int modifiers = KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK;
		KeyEvent pressed = new KeyEvent(jf, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, KeyEvent.VK_F,
				KeyEvent.CHAR_UNDEFINED);
		KeyEvent typed = new KeyEvent(jf, KeyEvent.KEY_TYPED, System.currentTimeMillis(), modifiers,
				KeyEvent.VK_UNDEFINED, 'F');
		KeyEvent released = new KeyEvent(jf, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), modifiers,
				KeyEvent.VK_F, KeyEvent.CHAR_UNDEFINED);
		jf.dispatchEvent(pressed);
		jf.dispatchEvent(typed);
		jf.dispatchEvent(released);
	}

	public synchronized static void reconnectAccount() {
		JFrame jf = MainWindowManager.mainWindowManager().getMainWindow();

		int modifiers = KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK;
		KeyEvent pressed = new KeyEvent(jf, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, KeyEvent.VK_R,
				KeyEvent.CHAR_UNDEFINED);
		KeyEvent typed = new KeyEvent(jf, KeyEvent.KEY_TYPED, System.currentTimeMillis(), modifiers,
				KeyEvent.VK_UNDEFINED, 'R');
		KeyEvent released = new KeyEvent(jf, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), modifiers,
				KeyEvent.VK_R, KeyEvent.CHAR_UNDEFINED);
		jf.dispatchEvent(pressed);
		jf.dispatchEvent(typed);
		jf.dispatchEvent(released);
	}

	private static void createToolkitListener() {
		Toolkit.getDefaultToolkit().addAWTEventListener(new TwsListener(createWindowHandlers()),
				AWTEvent.WINDOW_EVENT_MASK);
	}

	private static List<WindowHandler> createWindowHandlers() {
		List<WindowHandler> windowHandlers = new ArrayList<WindowHandler>();

		windowHandlers.add(new AcceptIncomingConnectionDialogHandler());
		windowHandlers.add(new BlindTradingWarningDialogHandler());
		// windowHandlers.add(new ExitSessionFrameHandler());
		windowHandlers.add(new LoginFrameHandler());
		windowHandlers.add(new GatewayLoginFrameHandler());
		windowHandlers.add(new MainWindowFrameHandler());
		windowHandlers.add(new GatewayMainWindowFrameHandler());
		windowHandlers.add(new NewerVersionDialogHandler());
		windowHandlers.add(new NewerVersionFrameHandler());
		windowHandlers.add(new NotCurrentlyAvailableDialogHandler());
		windowHandlers.add(new TipOfTheDayDialogHandler());
		windowHandlers.add(new NSEComplianceFrameHandler());
		windowHandlers.add(new PasswordExpiryWarningFrameHandler());
		windowHandlers.add(new GlobalConfigurationDialogHandler());
		windowHandlers.add(new TradesFrameHandler());
		windowHandlers.add(new ExistingSessionDetectedDialogHandler());
		windowHandlers.add(new ApiChangeConfirmationDialogHandler());
		windowHandlers.add(new SplashFrameHandler());
		windowHandlers.add(new SecurityCodeDialogHandler());
		windowHandlers.add(new ReloginDialogHandler());
		windowHandlers.add(new NonBrokerageAccountDialogHandler());
		windowHandlers.add(new ExitConfirmationDialogHandler());
		windowHandlers.add(new TradingLoginHandoffDialogHandler());
		windowHandlers.add(new LoginFailedDialogHandler());

		return windowHandlers;
	}

	private static String getJtsIniFilePath(String ibDir) throws IOException {
		return ibDir + File.separatorChar + "jts.ini";
	}

	private static String getTWSSettingsDirectory(String ibDir) throws IOException {
		Files.createDirectories(Paths.get(ibDir));
		return ibDir;
	}

	private static void startTws(Class<?> mainClass, String ibDir) throws ClassNotFoundException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException {
		if (Settings.settings().getBoolean("ShowAllTrades", false)) {
			Utils.showTradesLogWindow();
		}
		String[] twsArgs = new String[1];
		twsArgs[0] = getTWSSettingsDirectory(ibDir);
		mainClass.getMethod("main", String[].class).invoke(null, (Object) twsArgs);
	}

}
