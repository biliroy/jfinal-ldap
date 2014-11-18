package com.jfinal.plugin.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;


public class Config {
	
	private String ldapUrl;
	private String base;
	private String userDn;
	private String principal;
	
	private Hashtable<String, Object> baseEnv = new Hashtable<String, Object>();
	
	public static final String SUN_LDAP_POOLING_FLAG = "com.sun.jndi.ldap.connect.pool";
	public static final String DEFAULT_LDAP_CTX_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
	public static final String DEFAULT_AUTHENTICATION_TYPE = "simple";
	
	
	boolean devMode = false;
	
	public Config(){
		
	}
	
	public Config(String ldapUrl, String base, String userDn, String principal) {
		super();
		this.base = base;
		this.ldapUrl = ldapUrl;
		this.userDn = userDn;
		this.principal = principal;
	}
	
	public DirContext getCtx(){
		return doGetContext();
	}
	public boolean isDevMode() {
		return devMode;
	}
	public String getBase() {
		return base;
	}
	public void setBase(String base) {
		this.base = base;
	}
	public String getLdapUrl() {
		return ldapUrl;
	}
	public void setLdapUrl(String ldapUrl) {
		this.ldapUrl = ldapUrl;
	}
	public String getUserDn() {
		return userDn;
	}
	public void setUserDn(String userDn) {
		this.userDn = userDn;
	}
	public String getPrincipal() {
		return principal;
	}
	public void setPrincipal(String principal) {
		this.principal = principal;
	}
	
	private DirContext doGetContext() {
		Hashtable<String, Object> env = setupSimpleDirContextEnv();
		env.put(Context.SECURITY_PRINCIPAL, userDn);
		env.put(Context.SECURITY_CREDENTIALS, principal);
	    DirContext ctx = createContext(env);
	    return ctx;
	}
	
	protected DirContext createContext(Hashtable<String, Object> environment) {
		DirContext ctx = null;
		try{
			ctx = new InitialDirContext(environment);
			return ctx;
		}catch (Exception e) {
			LdapKit.closeContext(ctx);
			throw new LdapPluginException(e);
		}
	}
	
	private Hashtable<String, Object> setupSimpleDirContextEnv() {
		Hashtable<String, Object> env = new Hashtable<String, Object>(baseEnv);
		env.put(Context.INITIAL_CONTEXT_FACTORY, DEFAULT_LDAP_CTX_FACTORY);
		env.put(Context.SECURITY_AUTHENTICATION, DEFAULT_AUTHENTICATION_TYPE);
		env.put(Context.PROVIDER_URL, ldapUrl + base);
		return env;
	}
	 
	protected DirContext doGetContext(String principal, String credentials){
		Hashtable<String, Object> env = setupSimpleDirContextEnv();
		env.put(Context.SECURITY_PRINCIPAL, principal);
		env.put(Context.SECURITY_CREDENTIALS, credentials);
	    DirContext ctx = createContext(env);
	    return ctx;
	}
	
}
