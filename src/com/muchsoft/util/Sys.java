package com.muchsoft.util;

/**
 * <p>This class lets you determine which system your Java application runs on. Furthermore, it offers
 * methods for cross-platform queries for certain paths such as the preferences folder. It is used as
 * follows:
 * <p><pre> if (Sys.isMacOSX())
 * {
 *   // Mac OS X-specific code goes here...
 * }
 * else
 * {
 *   // code for other systems
 * }
 * 
 * // store user's configuration data
 * DataOutputStream out = new DataOutputStream(
 *                          new BufferedOutputStream(
 *                            new FileOutputStream(
 *                              new File( Sys.getPrefsDirectory(), "config.dat" )
 *                        )));
 * //...</pre>
 * <p>The latest Sys version can be found at
 * <a href="http://www.muchsoft.com/java/">http://www.muchsoft.com/java/</a>.
 * <p>Copyright 1998-2004 by Thomas Much, <a href="mailto:thomas@muchsoft.com">thomas@muchsoft.com</a>.
 * <br>This class is free for commercial and non-commercial use,
 * as long as you do not distribute modified versions of the source code.
 * If you have any suggestions, bug reports or feature requests, let me know.
 * <p><b>Version History:</b></p>
 * <dl>
 *   <dt> 2004-10-13
 *   <dd> Added a new package, com.muchsoft.util.mac, and a new class, {@link Mac}.
 *   <dt> 2004-05-04
 *   <dd> isMacOSX() now matches <a href="http://developer.apple.com/technotes/tn2002/tn2110.html">http://developer.apple.com/technotes/tn2002/tn2110.html</a>
 *   <dt> 2003-12-02
 *   <dd> First public release.
 * </dl>
 *
 * @author Thomas Much
 * @version 2004-10-13
 */

public class Sys {


private static final boolean ismacos;
private static final boolean ismacosx;
private static final boolean islinux;
private static final boolean iswindows;
private static final boolean isos2;

private static final String homeDir = System.getProperty("user.home");
private static final String workDir = System.getProperty("user.dir");
private static final String prefDir;
private static final String localPrefDir;
private static final String javaHome;



static {

	String osname = System.getProperty("os.name");
	String vendor = System.getProperty("java.vendor");
	String jhome  = System.getProperty("java.home");

	ismacosx  = osname.toLowerCase().startsWith("mac os x");
	ismacos   = (!ismacosx) && ((vendor.indexOf("Apple") >= 0) || (osname.indexOf("Mac OS") >= 0));
	islinux   = (osname.indexOf("Linux") >= 0);
	iswindows = (osname.indexOf("Windows") >= 0);
	isos2     = (osname.indexOf("OS/2") >= 0);
	
	if (ismacosx)
	{
		String pref      = homeDir + "/Library/Preferences";
		String localPref = "/Library/Preferences";
		
/*		// this way we'd do it ultra-correctly on Mac OS X / Java 1.4,
		// but then all Java apps would get a menu bar, which might not
		// be suitable for all situations

		try
		{
			Class fileman = Class.forName("com.apple.eio.FileManager");
			
			java.lang.reflect.Method findFolder =
					fileman.getMethod("findFolder", new Class[] { short.class, int.class });

			final int   kPreferencesFolderType = 0x70726566;
			final short kUserDomain            = -32763;
			final short kLocalDomain           = -32765;
			
			pref = (String)findFolder.invoke(null,
						new Object[] { new Short(kUserDomain), new Integer(kPreferencesFolderType) });

			localPref = (String)findFolder.invoke(null,
						new Object[] { new Short(kLocalDomain), new Integer(kPreferencesFolderType) });
		}
		catch (Exception e) {} */

		prefDir      = pref;
		localPrefDir = localPref;

		if ((jhome == null) || (jhome.length() == 0))
		{
			jhome = "/Library/Java/Home";
		}
	}
	else
	{
		prefDir      = homeDir;
		localPrefDir = (islinux) ? "/etc" : workDir;
	}
	
	javaHome = jhome;
}



private Sys() { }



/**
 * @return <code>true</code>, if the application is running on Mac OS 8/9, <code>false</code> otherwise
 */

public static boolean isMacOS() {

	return ismacos;
}



/**
 * @return <code>true</code>, if the application is running on Mac OS X, <code>false</code> otherwise
 */

public static boolean isMacOSX() {

	return ismacosx;
}



/**
 * @return <code>true</code>, if the application is running on a Mac (OS 8, 9 or X), <code>false</code> otherwise
 */

public static boolean isAMac() {

	return (ismacosx || ismacos);
}



/**
 * @return <code>true</code>, if the application is running on Linux, <code>false</code> otherwise
 */

public static boolean isLinux() {

	return islinux;
}



/**
 * @return <code>true</code>, if the application is running on Windows, <code>false</code> otherwise
 */

public static boolean isWindows() {

	return iswindows;
}



/**
 * @return <code>true</code>, if the application is running on OS/2, <code>false</code> otherwise
 */

public static boolean isOS2() {

	return isos2;
}



/**
 * The home directory contains the user's data and applications. On UNIX systems this directory is denoted
 * by <code>~</code> and can be queried through the system property <code>user.home</code>.
 * @return the user's home directory without a trailing path separator
 */

public static String getHomeDirectory() {

	return homeDir;
}



/**
 * The directory from which the application was launched is called the working directory. Its path can
 * be queried through the system property <code>user.dir</code>.
 * @return the application's working directory without a trailing path separator
 */

public static String getWorkingDirectory() {

	return workDir;
}



/**
 * The preferences directory contains the user's configuration files. On Mac OS X, this method returns
 * <code>~/Library/Preferences</code>, on all other systems the user's home directory is used.
 * @return the user's preferences directory without a trailing path separator
 */

public static String getPrefsDirectory() {

	return prefDir;
}



/**
 * The local preferences directory contains configuration files that are shared by all users on the computer.
 * On Mac OS X, this method returns <code>/Library/Preferences</code>, on Linux <code>/etc</code>. On all
 * other systems the application's working directory is used.
 * <i>Please note: There is no guarantee that your application has permission to use this directory!</i>
 * @return the shared preferences directory (without a trailing path separator) of all users on a local computer
 */

public static String getLocalPrefsDirectory() {

	return localPrefDir;
}



/**
 * The Java home directory contains the <code>bin</code> subdirectory and is needed to invoke the Java tools
 * at runtime. It is specified by the environment variable <code>$JAVA_HOME</code> and can be queried through
 * the system property <code>java.home</code>. If the variable is not set properly, this method returns
 * <code>/Library/Java/Home</code> on Mac OS X.
 * @return the Java home directory without a trailing path separator
 */

public static String getJavaHome() {

	return javaHome;
}



/**
 * This is a small test case for all the methods in this class.
 * On Mac OS X you can launch it by simply double-clicking the jar file (output goes to the Console).
 * On other systems, you can start it with <code>java -classpath .:Sys.jar com.muchsoft.util.Sys</code>
 * (or <code>java -jar Sys.jar</code>)
 * @param args not used
 */

public static void main(String[] args) {

	System.out.println();
	System.out.println("** com.muchsoft.util.Sys version 2004-10-13");
	System.out.println("**");
	System.out.println("** JavaHome:   " + getJavaHome());
	System.out.println("** AppWork:    " + getWorkingDirectory());
	System.out.println("** UserHome:   " + getHomeDirectory());
	System.out.println("** UserPrefs:  " + getPrefsDirectory());
	System.out.println("** LocalPrefs: " + getLocalPrefsDirectory());
	System.out.println("**");
	System.out.print(  "** System:     ");

	if (isMacOSX())
	{
		System.out.println("Mac OS X");
	}
	else if (isMacOS())
	{
		System.out.println("Mac OS 8/9");
	}
	else if (isLinux())
	{
		System.out.println("Linux");
	}
	else if (isWindows())
	{
		System.out.println("Windows");
	}
	else if (isOS2())
	{
		System.out.println("OS/2");
	}
	else
	{
		System.out.println("unknown");
	}

	System.out.println("**             " + System.getProperty("os.name"));
	System.out.println("** Java:       " + System.getProperty("java.version"));
	System.out.println("**             " + System.getProperty("java.runtime.version"));
	System.out.println("**             " + System.getProperty("java.vm.version"));
	System.out.println("**             " + System.getProperty("java.vendor"));
	System.out.println("** MRJ:        " + System.getProperty("mrj.version"));
	System.out.println();
}

}
