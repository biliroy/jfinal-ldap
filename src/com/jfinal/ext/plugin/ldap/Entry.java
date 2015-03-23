package com.jfinal.ext.plugin.ldap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Entry implements Serializable{
	
	private static final long serialVersionUID = 8903586436824980214L;
	
	private Map<String, Object> attrs = new HashMap<String, Object>();
	
	public Map<String, Object> getAttrs() {
		return attrs;
	}

	public void setAttrs(Map<String, Object> attrs) {
		this.attrs = attrs;
	}
	
	public byte[] getBytes(String attr) {
		return (byte[])attrs.get(attr);
	}
	
	public Set<java.util.Map.Entry<String, Object>> getAttrsEntrySet() {
		return attrs.entrySet();
	}
	
	public String[] getAttrNames() {
		Set<String> attrNameSet = attrs.keySet();
		return attrNameSet.toArray(new String[attrNameSet.size()]);
	}
	
	public Object[] getAttrValues() {
		java.util.Collection<Object> attrValueCollection = attrs.values();
		return attrValueCollection.toArray(new Object[attrValueCollection.size()]);
	}
	
	public Entry remove(String column) {
		attrs.remove(column);
		return this;
	}
	
	public Entry remove(String... columns) {
		if (columns != null)
			for (String c : columns)
				attrs.remove(c);
		return this;
	}
	
	public Entry clear() {
		attrs.clear();
		return this;
	}
	
	public Entry set(String column, Object value) {
		attrs.put(column, value);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String column) {
		return (T)attrs.get(column);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String column, Object defaultValue) {
		Object result = attrs.get(column);
		return (T)(result != null ? result : defaultValue);
	}
	
	public String getStr(String column) {
		return (String)attrs.get(column);
	}
	
	public String getFdn(){
		return getStr(Builder.NS_FULL);
	}
	
	public String getSdn(){
		return getStr(Builder.NS_SIMPLE);
	}
	
	public String getPdn(){
		return getStr(Builder.NS_PARENT);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append(" {");
		boolean first = true;
		for (java.util.Map.Entry<String, Object> e : attrs.entrySet()) {
			if (first)
				first = false;
			else
				sb.append(", ");
			Object value = e.getValue();
			if (value != null)
				value = value.toString();
			sb.append(e.getKey()).append(":").append(value);
		}
		sb.append("}");
		return sb.toString();
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Entry))
            return false;
		if (o == this)
			return true;
		return this.attrs.equals(((Entry)o).attrs);
	}
	
	public int hashCode() {
		return attrs == null ? 0 : attrs.hashCode();
	}
	
	public String toJson() {
		return com.jfinal.kit.JsonKit.toJson(attrs, 4);
	}
	
}
