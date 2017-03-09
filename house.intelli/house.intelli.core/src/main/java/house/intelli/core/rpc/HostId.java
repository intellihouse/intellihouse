package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;
import static house.intelli.core.util.StringUtil.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unique identifier of a computer taking part in the RPC.
 * <p>
 * This identifier is usually read via {@link System#getenv(String)} with name="HOSTNAME".
 * Typical values are thus sth. like "calypso" or "my-computer-12345". It is possible, though,
 * to manually configure a fully-qualified host name as host-id, containing a domain name
 * (e.g. "calypso.codewizards.co" or "my-computer-12345.somenetwork.de").
 *
 * @author mn
 */
@XmlJavaTypeAdapter(type=HostId.class, value=HostId.HostIdXmlAdapter.class)
public class HostId {
	private static final Logger logger = LoggerFactory.getLogger(HostId.class);

	public static final Pattern HOSTID_PATTERN = Pattern.compile("[A-Za-z0-9_\\-\\.]*");

	public static final HostId CENTRAL = new HostId("central");

	/**
	 * Gets the {@code HostId} of this computer.
	 * <p>
	 * This method consults the following in this order:
	 * <ol>
	 * <li>Read the environment variable "HOSTNAME".
	 * <li>Run the program "hostname". This program should work on most (or all?) Unix-like systems.
	 * <li>Read the file "/etc/hostname". This file should exist on most (or all?) Unix-like systems.
	 * </ol>
	 * @return the {@link HostId} of this computer. Never <code>null</code>.
	 * @throws IllegalStateException if the hostname is illegal (does not match {@link #HOSTID_PATTERN}) or
	 * if it cannot be determined.
	 */
	public static HostId getLocalHostId() throws IllegalStateException {

		///////// Read environment variable 'HOSTNAME' /////////
		// This env var is provided by bash and may be missing, if another shell is used.

		final String envName = "HOSTNAME";

		String hostName = trim(System.getenv(envName));
		if (! isEmpty(hostName)) {
			try {
				HostId hostId = new HostId(hostName);
				return hostId;
			} catch (Exception x) {
				throw new IllegalStateException(String.format("Environment variable '%s' contains illegal value '%s': %s",
						envName, hostName, x), x);
			}
		}

		logger.warn("Environment variable '{}' is missing or empty.", envName);


		///////// Call program 'hostname' /////////

		final String program = "hostname";
		try {
			Process process = Runtime.getRuntime().exec(program);
			process.waitFor(10, TimeUnit.SECONDS);
			try (InputStream in = process.getInputStream()) {
				BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				hostName = trim(r.readLine());
			}

			if (! isEmpty(hostName)) {
				try {
					HostId hostId = new HostId(hostName);
					return hostId;
				} catch (Exception x) {
					logger.warn("Program '{}' returned illegal value '{}': {}", program, hostName, x);
				}
			}
			else
				logger.warn("Program '{}' returned an empty result.", program);

		} catch (Exception e) {
			logger.warn("Calling program '" + program + "' failed: " + e, e);
		}


		///////// Read file '/etc/hostname' /////////

		final File file = new File("/etc/hostname");
		if (file.exists()) {
			try {
				try (InputStream in = new FileInputStream(file)) {
					BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
					hostName = trim(r.readLine());
				}
			} catch (Exception e) {
				logger.warn("Reading file '" + file + "' failed: " + e, e);
			}

			if (! isEmpty(hostName)) {
				try {
					HostId hostId = new HostId(hostName);
					return hostId;
				} catch (Exception x) {
					logger.warn("File '{}' contained illegal value '{}': {}", file, hostName, x);
				}
			}
			else
				logger.warn("File '{}' is empty or contains an empty first line.", file);
		}
		else
			logger.warn("File '" + file + "' does not exist!");

		throw new IllegalStateException(String.format("Could not determine local hostname! Tried environment variable '%s', program '%s' and file '%s'!",
				envName, program, file));
	}

	public static class HostIdXmlAdapter extends XmlAdapter<String, HostId> {
		@Override
		public HostId unmarshal(final String v) throws Exception {
			return new HostId(v);
		}
		@Override
		public String marshal(final HostId v) throws Exception {
			return v.toString();
		}
	}

	private final String id;

	public HostId(String id) {
		this.id = assertNotEmpty(id, "id");
		if (! HOSTID_PATTERN.matcher(id).matches())
			throw new IllegalArgumentException(String.format("HostId '%s' is not valid!", id));
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) return true;

		if (obj == null) return false;

		if (getClass() != obj.getClass()) return false;

		final HostId other = (HostId) obj;
		return this.id.equals(other.id); // guaranteed to be never null!
	}
}
