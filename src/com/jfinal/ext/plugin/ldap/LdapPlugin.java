package com.jfinal.ext.plugin.ldap;

import javax.naming.directory.DirContext;

import com.jfinal.plugin.IPlugin;

public class LdapPlugin implements IPlugin {
	
	private Config config = null;
	
	private boolean isStarted = false;
	
	public LdapPlugin(String ldapUrl, String base, String userDn,
			String password) {
		super();
		this.config = new Config(ldapUrl, base, userDn, password);
	}
	
	@Override
	public boolean start() {
		if (isStarted){
			return true;
		}
		LdapKit.addConfig(config);
		DirContext ctx = null;
		try{
			ctx = config.getCtx();
			return true;
		}catch(Exception e){
			throw new LdapPluginException(e);
		}finally{
			LdapKit.closeContext(ctx);
		}
	}
	
	@Override
	public boolean stop() {
		isStarted = false;
		return true;
	}

	public Config getConfig() {
		return config;
	}
}
