package com.jfinal.ext.plugin.ldap;

public class LdapPluginException extends RuntimeException{

	private static final long serialVersionUID = 1L;

	public LdapPluginException() {
		super();
	}

	public LdapPluginException(String message, Throwable cause) {
		super(message, cause);
	}

	public LdapPluginException(String message) {
		super(message);
	}

	public LdapPluginException(Throwable cause) {
		super(cause);
	}
}
