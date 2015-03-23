package com.jfinal.ext.plugin.ldap.filter;

/**
 * Common interface for LDAP filters.
 * 
 * @author Adam Skogman
 * @see <a href="http://www.ietf.org/rfc/rfc1960.txt">RFC 1960: A String
 * Representation of LDAP Search Filters< /a>
 */
public interface Filter {

	/**
	 * Encodes the filter to a String.
	 * 
	 * @return The encoded filter in the standard String format
	 */
	String encode();

	/**
	 * Encodes the filter to a StringBuffer.
	 * 
	 * @param buf The StringBuffer to encode the filter to
	 * @return The same StringBuffer as was given
	 */
	StringBuffer encode(StringBuffer buf);

	/**
	 * All filters must implement equals.
	 * 
	 * @param o
	 * @return <code>true</code> if the objects are equal.
	 */
	boolean equals(Object o);

	/**
	 * All filters must implement hashCode.
	 * 
	 * @return the hash code according to the contract in
	 * {@link Object#hashCode()}
	 */
	int hashCode();
}
