package com.jfinal.ext.plugin.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

public class EntryBuilder extends Builder{
	
	public static final List<Entry> build(NamingEnumeration<SearchResult> ne) throws Exception{
		List<Entry> result = new ArrayList<Entry>();
		for(; ne.hasMoreElements();){
			Entry entry = new Entry();
			Map<String, Object> entryAttrs = entry.getAttrs();
			SearchResult searchResult = ne.nextElement();
			Attributes attrs = searchResult.getAttributes();
			NamingEnumeration<String> ids = attrs.getIDs();
			for(; ids.hasMoreElements();){
				String name = ids.nextElement();
				setupAttrs(attrs, entryAttrs, name);
			}
			setupNames(entryAttrs, searchResult);
			result.add(entry);
		}
		return result;
	}
}
