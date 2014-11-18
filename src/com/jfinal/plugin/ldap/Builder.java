package com.jfinal.plugin.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

public class Builder {
	
	public static void setupAttrs(Attributes attrs, Map<String, Object> attrMap, String name ){
		List<Object> values = new ArrayList<Object>();
		try{
			LdapKit.collectAttributeValues(attrs, name, values);
		}catch(Exception e){
		}
		if(values == null || values.size()==0){
			attrMap.put(name, null);
		}else if(values.size()==1){
			attrMap.put(name, values.get(0));
		}else{
			attrMap.put(name, values);
		}
	}
	
	/**
	 * setup fullName, simpleName, parentName into attrMap
	 * @param attrMap
	 * @param searchResult
	 * @throws Exception
	 */
	public static void setupNames(Map<String, Object> attrMap, SearchResult searchResult) throws Exception{
		String fullDN = searchResult.getNameInNamespace();
		attrMap.put(LableAware.NS_FULL, fullDN);
		LdapName ldapName = LdapKit.newLdapName(fullDN);
		ldapName.remove(ldapName.getRdns().size()-1);
		attrMap.put(LableAware.NS_PARENT, ldapName.toString());
		attrMap.put(LableAware.NS_SIMPLE, searchResult.getName());
	}
}
