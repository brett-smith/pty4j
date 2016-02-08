package com.pty4j.unix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.scijava.nativelib.DefaultJniExtractor;
import org.scijava.nativelib.JniExtractor;

import com.sun.jna.Native;

/**
 * @author brett smith
 */
public class NARPtyExecutor implements PtyExecutor {

	private final Pty4J myPty4j;

	public NARPtyExecutor() throws Exception {
		myPty4j = (Pty4J) Native.loadLibrary(locateLibrary(), Pty4J.class);
	}

	@Override
	public int execPty(String full_path, String[] argv, String[] envp, String dirpath, String pts_name, int fdm,
			String err_pts_name, int err_fdm, boolean console, int euid) {
		return myPty4j.exec_pty(full_path, argv, envp, dirpath, pts_name, fdm, err_pts_name, err_fdm, console, euid);
	}

	public interface Pty4J extends com.sun.jna.Library {
		int exec_pty(String full_path, String[] argv, String[] envp, String dirpath, String pts_name, int fdm,
				String err_pts_name, int err_fdm, boolean console, int euid);
	}

	public static String locateLibrary() {
		String fileName = "pty4j";
		
		try {
			Properties p = new Properties();
			InputStream in = NARPtyExecutor.class.getResourceAsStream("/pty4j.properties");
			try {
				p.load(in);
				fileName = "pty4j-" + p.getProperty("pty4j.version");
			}
			finally {
				in.close();
			}
		} catch (IOException ex) {
			System.err.println(NARPtyExecutor.class.getName() + " : Error reading manifest file information");
			ex.printStackTrace();
		}

		final String mapped = System.mapLibraryName(fileName);
		final String[] aols = getAOLs();
		final ClassLoader loader = NARPtyExecutor.class.getClassLoader();
		final File unpacked = getUnpackedLibPath(loader, aols, fileName, mapped);
		if (unpacked != null) {
			return unpacked.getPath();
		} else
			try {
				final String libPath = getLibPath(loader, aols, mapped);
				final JniExtractor extractor = new DefaultJniExtractor(NARPtyExecutor.class,
						System.getProperty("java.io.tmpdir"));
				return extractor.extractJni(libPath, fileName).getPath();
			} catch (final Exception e) {
				e.printStackTrace();
				throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
			}
	}

	private static String[] getAOLs() {
		final String ao = System.getProperty("os.arch") + "-" + System.getProperty("os.name").replaceAll(" ", "");

		// choose the list of known AOLs for the current platform
		if (ao.startsWith("i386-Linux")) {
			return new String[] { "i386-Linux-ecpc", "i386-Linux-gpp", "i386-Linux-icc", "i386-Linux-ecc",
					"i386-Linux-icpc", "i386-Linux-linker", "i386-Linux-gcc" };
		} else if (ao.startsWith("x86-Windows")) {
			return new String[] { "x86-Windows-linker", "x86-Windows-gpp", "x86-Windows-msvc", "x86-Windows-icl",
					"x86-Windows-gcc" };
		} else if (ao.startsWith("amd64-Linux")) {
			return new String[] { "amd64-Linux-gpp", "amd64-Linux-icpc", "amd64-Linux-gcc", "amd64-Linux-linker" };
		} else if (ao.startsWith("amd64-Windows")) {
			return new String[] { "amd64-Windows-gpp", "amd64-Windows-msvc", "amd64-Windows-icl",
					"amd64-Windows-linker", "amd64-Windows-gcc" };
		} else if (ao.startsWith("amd64-FreeBSD")) {
			return new String[] { "amd64-FreeBSD-gpp", "amd64-FreeBSD-gcc", "amd64-FreeBSD-linker" };
		} else if (ao.startsWith("ppc-MacOSX")) {
			return new String[] { "ppc-MacOSX-gpp", "ppc-MacOSX-linker", "ppc-MacOSX-gcc" };
		} else if (ao.startsWith("x86_64-MacOSX")) {
			return new String[] { "x86_64-MacOSX-icc", "x86_64-MacOSX-icpc", "x86_64-MacOSX-gpp",
					"x86_64-MacOSX-linker", "x86_64-MacOSX-gcc" };
		} else if (ao.startsWith("ppc-AIX")) {
			return new String[] { "ppc-AIX-gpp", "ppc-AIX-xlC", "ppc-AIX-gcc", "ppc-AIX-linker" };
		} else if (ao.startsWith("i386-FreeBSD")) {
			return new String[] { "i386-FreeBSD-gpp", "i386-FreeBSD-gcc", "i386-FreeBSD-linker" };
		} else if (ao.startsWith("sparc-SunOS")) {
			return new String[] { "sparc-SunOS-cc", "sparc-SunOS-CC", "sparc-SunOS-linker" };
		} else if (ao.startsWith("arm-Linux")) {
			return new String[] { "arm-Linux-gpp", "arm-Linux-linker", "arm-Linux-gcc" };
		} else if (ao.startsWith("x86-SunOS")) {
			return new String[] { "x86-SunOS-g++", "x86-SunOS-linker" };
		} else if (ao.startsWith("i386-MacOSX")) {
			return new String[] { "i386-MacOSX-gpp", "i386-MacOSX-gcc", "i386-MacOSX-linker" };
		} else {
			throw new RuntimeException("Unhandled architecture/OS: " + ao);
		}
	}

	private static File getUnpackedLibPath(final ClassLoader loader, final String[] aols, final String fileName,
			final String mapped) {
		final String classPath = NARPtyExecutor.class.getName().replace('.', '/') + ".class";
		final URL url = loader.getResource(classPath);
		if (url == null || !"file".equals(url.getProtocol()))
			return null;
		final String path = url.getPath();
		final String prefix = path.substring(0, path.length() - classPath.length()) + "../nar/" + fileName + "-";
		for (final String aol : aols) {
			final File file = new File(prefix + aol + "-shared/lib/" + aol + "/shared/" + mapped);
			if (file.isFile())
				return file;
		}
		return null;
	}

	private static String getLibPath(final ClassLoader loader, final String[] aols, final String mapped) {
		for (final String aol : aols) {
			final String libPath = "lib/" + aol + "/shared/";
			if (loader.getResource(libPath + mapped) != null)
				return libPath;
		}
		throw new RuntimeException("Library '" + mapped + "' not found!");
	}

}
