package com.jfinal.plugin.ldap;

import javax.naming.directory.SearchControls;

public abstract class LableAware {
	
	/** common init method */
	
	public abstract String baseDN();
	public abstract String rdnKey();
	public abstract String[] searchAttributes();
	public abstract String[] requiredAttributes();
	public abstract String[] buildObjectClasses();
	public abstract SearchControls searchControls();
	
	
}
