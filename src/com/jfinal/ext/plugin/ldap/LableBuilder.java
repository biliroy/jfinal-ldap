package com.jfinal.plugin.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

public class LableBuilder extends Builder{
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final <T> List<T> build(NamingEnumeration<SearchResult> ne, Class<? extends Lable> lableClass)
			throws Exception {
		List<T> result = new ArrayList<T>();
		for(; ne.hasMoreElements();){
			Lable<?> lable = lableClass.newInstance();
			Map<String, Object> lableAttrs = lable.getAttrs();
			SearchResult searchResult = ne.nextElement();
			Attributes attrs = searchResult.getAttributes();
			String[] searchAttrs = lable.searchAttributes();
			if(searchAttrs==null || searchAttrs.length==0){
				NamingEnumeration<String> ids = attrs.getIDs();
				for(; ids.hasMoreElements();){
					String name = ids.nextElement();
					setupAttrs(attrs, lableAttrs, name);
				}
			}else{
				for(String name : searchAttrs){
					setupAttrs(attrs, lableAttrs, name);
				}
			}
			setupNames(lableAttrs, searchResult);
			lable.setAttributes(attrs);
			result.add((T)lable);
		}
		return result;
	}
	
}
