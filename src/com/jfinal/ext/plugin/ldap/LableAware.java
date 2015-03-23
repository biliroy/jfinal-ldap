package com.jfinal.ext.plugin.ldap;

import javax.naming.directory.SearchControls;

public abstract class LableAware {
	
	public abstract String baseDN();
	
	/**
	 * 顶层RDN（uid,mail等）
	 * @return
	 */
	public abstract String rdnKey();
	
	/**
	 * 查询后返回的属性（可为 null，返回所有，
	 * 但不包括类似 createTimestamp creatorsName 等系统属性..）
	 */
	public abstract String[] searchAttributes();
	
	/**
	 * 构建时必要的属性（cn,sn,uid等）
	 * @return 
	 */
	public abstract String[] requiredAttributes();
	
	/**
	 * 构建该ObjectClass的值（top,person,inetOrgPerson等）
	 * @return
	 */
	public abstract String[] buildObjectClasses();
	
	/**
	 * 查询的SearchControls类型(OBJECT_SCOPE,ONELEVEL_SCOPE,SUBTREE_SCOPE)
	 * @return
	 */
	public abstract SearchControls searchControls();
	
	
}
