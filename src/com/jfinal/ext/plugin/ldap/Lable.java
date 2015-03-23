package com.jfinal.ext.plugin.ldap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import com.jfinal.ext.plugin.ldap.filter.Filter;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class Lable<M extends Lable> extends LableAware implements Serializable {
	
	private static final long serialVersionUID = -3093756736204638247L;
	
	private Map<String, Object> attrs = new HashMap<String, Object>();
	
	private Attributes attributes;
	
	void setAttributes(Attributes attributes){
		this.attributes = attributes;
	}
	
	protected Config getConfig() {
		return LdapKit.getConfig();
	}
	
	private DirContext getContext() {
		return getConfig().getCtx();
	}
	
	private Set<String> modifyFlag = new HashSet<String>();
	
	
	private Set<String> getModifyFlag(){
		return modifyFlag;
	}
	
	public M set(String attr, Object value) {
		if (containsAttr(attr)) {
			attrs.put(attr, value);
			getModifyFlag().add(attr);
			return (M)this;
		}
		throw new LdapPluginException("The attribute name is not exists: " + attr);
	}
	
	public M put(String key, Object value) {
		attrs.put(key, value);
		return (M)this;
	}
	
	public <T> T get(String attr) {
		return (T)(attrs.get(attr));
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
	
	
	public <T> T get(String attr, Object defaultValue) {
		Object result = attrs.get(attr);
		return (T)(result != null ? result : defaultValue);
	}
	
	public String getStr(String attr) {
		return (String)attrs.get(attr);
	}
	
	public byte[] getBytes(String attr) {
		return (byte[])attrs.get(attr);
	}
	
	protected Map<String, Object> getAttrs() {
		return attrs;
	}
	
	public Set<java.util.Map.Entry<String, Object>> getAttrsEntrySet() {
		return attrs.entrySet();
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
			if (value != null){
				if(value instanceof byte[]){
					value = new String((byte[])value);
				}
				value = value.toString();
			}
			sb.append(e.getKey()).append(":").append(value);
		}
		sb.append("}");
		return sb.toString();
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Lable))
            return false;
		if (o == this)
			return true;
		return this.attrs.equals(((Lable)o).attrs);
	}
	
	public int hashCode() {
		return (attrs == null ? 0 : attrs.hashCode()) ^ (getModifyFlag() 
				== null ? 0 : getModifyFlag().hashCode());
	}
	

	public String[] getAttrNames() {
		Set<String> attrNameSet = attrs.keySet();
		return attrNameSet.toArray(new String[attrNameSet.size()]);
	}

	public Object[] getAttrValues() {
		java.util.Collection<Object> attrValueCollection = attrs.values();
		return attrValueCollection.toArray(new Object[attrValueCollection.size()]);
	}
	

	public String toJson() {
		return com.jfinal.kit.JsonKit.toJson(attrs, 4);
	}
	

	public M setAttrs(M m) {
		return setAttrs(m.getAttrs());
	}
	

	public M setAttrs(Map<String, Object> attrs) {
		for (java.util.Map.Entry<String, Object> e : attrs.entrySet())
			set(e.getKey(), e.getValue());
		return (M)this;
	}
	
	public M remove(String attr) {
		attrs.remove(attr);
		getModifyFlag().remove(attr);
		return (M)this;
	}
	
	public M remove(String... attrs) {
		if (attrs != null)
			for (String a : attrs) {
				this.attrs.remove(a);
				this.getModifyFlag().remove(a);
			}
		return (M)this;
	}
	
	public M removeNullValueAttrs() {
		Iterator<java.util.Map.Entry<String, Object>> it = attrs.entrySet().iterator();
		for (; it.hasNext();) {
			java.util.Map.Entry<String, Object> e = it.next();
			if (e.getValue() == null) {
				it.remove();
				getModifyFlag().remove(e.getKey());
			}
		}
		return (M)this;
	}
	
	public M clear() {
		attrs.clear();
		modifyFlag.clear();
		return (M)this;
	}
	
	
	public boolean update() throws Exception {
		if (modifyFlag.isEmpty()){
			return false;
		}
		ModificationItem[] mods = getModificationItems();
		if (mods.length <= 0) {	// Needn't update
			return false;
		}
		DirContext context = getContext();
		try {
			context.modifyAttributes(getSdn() + "," + baseDN(), mods);
		} catch (javax.naming.NamingException ex) {
			throw new LdapPluginException(ex);
		}finally{
			LdapKit.closeContext(context);
		}
		getModifyFlag().clear();
		return true;
	}
	
	/**
	 * 
	 * @param newDN the new full dn
	 */
	public void rename(String newDN){
		DirContext ctx = null;
		try{
			ctx = getContext();
			ctx.rename(getFdn(), newDN);
		}catch(Exception e){
			throw new LdapPluginException(e);
		}finally{
			LdapKit.closeContext(ctx);
		}
	}
	
	public List<M> find(Filter filter)throws Exception{
		NamingEnumeration<SearchResult> ne = null;
		DirContext context = null;
		try {
			context = getContext();
			ne = context.search(baseDN(), filter.encode(), searchAttributes(), searchControls());
		} catch (javax.naming.NamingException ex) {
			throw new LdapPluginException(ex);
		}finally{
			LdapKit.closeContext(context);
		}
		return LableBuilder.build(ne, this.getClass());
	}
	
	private boolean containsAttr(String attr){
		if(searchAttributes()!=null){
			return Arrays.asList(searchAttributes()).contains(attr);
		}
		return true;
	}
	
	
	public boolean delete(){
		DirContext ctx = null;
		try{
			ctx = getContext();
			ctx.unbind(getFdn());
			return true;
		}catch(Exception e){
			throw new LdapPluginException(e);
		}finally{
			LdapKit.closeContext(ctx);
		}
	}
	
	public boolean save(){
		DirContext context = null;
		for(String attr : requiredAttributes()){
			Object o = get(attr);
			if(o==null){
				throw new LdapPluginException("please set the attribute:"+attr +"'s value");
			}
		}
		String name = rdnKey() + "=" + getStr(rdnKey())+"," + baseDN();
		try {
			context = getContext();
			context.bind(name, null, setupCreateAttributes());
		}catch (javax.naming.NamingException ex) {
			throw new LdapPluginException(ex);
		}finally{
			LdapKit.closeContext(context);
		}
		return true;
	}
	
	private BasicAttributes setupCreateAttributes(){
		BasicAttributes basicAttrs = new BasicAttributes();
		BasicAttribute basicAttr = new BasicAttribute("objectclass");
		for(String d : buildObjectClasses()){
			basicAttr.add(d);
		}
		basicAttrs.put(basicAttr);
		Iterator<java.util.Map.Entry<String, Object>>  iter = getAttrsEntrySet().iterator();
		while(iter.hasNext()){
			java.util.Map.Entry<String, Object> e = iter.next();
			basicAttrs.put(e.getKey(), e.getValue());
		}
		return basicAttrs;
	}
	
	private ModificationItem[] getModificationItems() {
		if (modifyFlag.isEmpty()){
			return new ModificationItem[0];
		}
		List<ModificationItem> tmpList = new LinkedList<ModificationItem>();
		Set<String> modifySet = getModifyFlag();
		
		for (java.util.Map.Entry<String, Object> e : attrs.entrySet()) {
			String colName = e.getKey();
			if ( modifySet.contains(colName) && containsAttr(colName)) {
				collectModifications(colName, tmpList);
			}
		}
		return tmpList.toArray(new ModificationItem[tmpList.size()]);
	}
	
	private void collectModifications(String attrID, List<ModificationItem> modificationList){
		Attribute currentAttribute = attributes.get(attrID);
		Object changedValue =  attrs.get(attrID);
		if( attributeValueEquals(currentAttribute, changedValue)){
			// No changes
			return ;
		}
		
		if(currentAttribute!=null && (changedValue==null || "".equals(changedValue)) ){
			ModificationItem item = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(attrID));
			modificationList.add(item);
		}
		
		if((currentAttribute==null || currentAttribute.size() == 0) && changedValue!=null ){
			if(changedValue instanceof List){
				List<Object> updates = (List)changedValue;
				for(Object o : updates){
					ModificationItem item = new ModificationItem(DirContext.ADD_ATTRIBUTE, 
							new BasicAttribute(attrID,o));
					modificationList.add(item);
				}
			}else {
				ModificationItem item = new ModificationItem(DirContext.ADD_ATTRIBUTE, 
						new BasicAttribute(attrID, changedValue));
				modificationList.add(item);
			}
		}
		
		if(currentAttribute!=null && changedValue!=null){
			if(changedValue instanceof List){
				List<Object> updates = (List)changedValue;
				Attribute originalClone = (Attribute) currentAttribute.clone();
				Attribute addedValuesAttribute = new BasicAttribute(attrID);
				for(Object attrValue : updates){
		            if (!originalClone.remove(attrValue)) {
		                addedValuesAttribute.add(attrValue);
		            }
		        }
		        // We have now traversed and removed all values from the original that
		        // were also present in the new values. The remaining values in the
		        // original must be the ones that were removed.
		        if(originalClone.size() > 0 && originalClone.size() == updates.size()) {
		            // This is actually a complete replacement of the attribute values.
		            // Fall back to REPLACE
		            modificationList.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
		                    addedValuesAttribute));
		        } else {
		            if (originalClone.size() > 0) {
		                modificationList.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, 
		                		originalClone));
		            }
		            if (addedValuesAttribute.size() > 0) {
		                modificationList.add(new ModificationItem(DirContext.ADD_ATTRIBUTE,
		                        addedValuesAttribute));
		            }
		        }
				
			}else if (currentAttribute != null && currentAttribute.size() == 1 ) {
				// Replace single-vale attribute.
				modificationList.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, 
						new BasicAttribute(attrID, changedValue)));
			}
		}
	}
	
	private boolean attributeValueEquals(Attribute attr, Object obj){
		if(attr==null && obj==null){
			return true;
		}
		if(attr!=null){
			try {
				Object ob = attr.get();
				return ob.equals(obj);
			} catch (NamingException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
}
