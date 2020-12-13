package ibcalpha.ibc;

import java.awt.AWTEvent;
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

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;

public class TWSManager {

	public synchronized static boolean isOpen() {
		JFrame jf = MainWindowManager.mainWindowManager().getMainWindow(0, TimeUnit.MILLISECONDS);
		return jf != null && jf.isDisplayable();
	}

	public synchronized static void open(Class<?> mainClass, String ibDir) throws IOException, ClassNotFoundException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		createToolkitListener();
		JtsIniManager.initialise(getJtsIniFilePath(ibDir));
		startTws(mainClass, ibDir);
	}

	public synchronized static void close() {
		(new StopTask(null)).run(); // run on the current thread
	}

	public synchronized static void enableAPI(int portNumber, boolean readOnly)
			throws InterruptedException, IbcException, ExecutionException {
		if (portNumber != 0)
			(new ConfigurationTask(new ConfigureTwsApiPortTask(portNumber))).executeAsync();
		(new ConfigurationTask(new ConfigureReadOnlyApiTask(readOnly))).executeAsync();

		final JDialog configDialog = ConfigDialogManager.configDialogManager().getConfigDialog();
		FutureTask<?> t = new FutureTask<>((Runnable) () -> {
			try {
				if (!Utils.selectConfigSection(configDialog, new String[] { "API", "Settings" }))
					// older versions of TWS don't have the Settings node below the API node
					Utils.selectConfigSection(configDialog, new String[] { "API" });

				JCheckBox cb = SwingUtils.findCheckBox(configDialog, "Enable ActiveX and Socket Clients");
				if (cb == null)
					throw new IbcException("could not find Enable ActiveX checkbox");

				if (!cb.isSelected()) {
					cb.doClick();
					SwingUtils.clickButton(configDialog, "OK");
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
	}

	public static synchronized void saveSettings() {
        Utils.invokeMenuItem(MainWindowManager.mainWindowManager().getMainWindow(), new String[] {"File", "Save Settings"});
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
		windowHandlers.add(new ExitSessionFrameHandler());
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

	private static void startTws(Class<?> mainClass, String ibDir) throws ClassNotFoundException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, IOException {
		if (Settings.settings().getBoolean("ShowAllTrades", false)) {
			Utils.showTradesLogWindow();
		}
		String[] twsArgs = new String[1];
		twsArgs[0] = getTWSSettingsDirectory(ibDir);
		mainClass.getMethod("main", String[].class).invoke(null, (Object) twsArgs);
	}

}
