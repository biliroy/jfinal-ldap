package com.jfinal.plugin.ldap;

import javax.naming.directory.SearchControls;

public abstract class LableAware {
	
	/** full dn */
	public static final String NS_FULL = "fdn";
	/** simple dn */
	public static final String NS_SIMPLE = "sdn";
	/** parent dn */
	public static final String NS_PARENT = "pdn";
	
	public abstract String baseDN();
	public abstract String rdnKey();
	public abstract String[] searchAttributes();
	public abstract String[] requiredAttributes();
	public abstract String[] buildObjectClasses();
	public abstract SearchControls searchControls();
	
}
