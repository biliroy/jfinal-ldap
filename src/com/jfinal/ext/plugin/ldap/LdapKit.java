package com.jfinal.ext.plugin.ldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfinal.plugin.activerecord.Page;

@SuppressWarnings("rawtypes")
public final class LdapKit {

	private static final Logger LOGGER = LoggerFactory.getLogger(LdapKit.class);
	private static Config config = new Config();
	
	private LdapKit() {

	}
	
	private enum AuthenticationStatus {
		
		SUCCESS(true),
		EMPTYRESULT(false),
		MANYRESULT(false),
		UNDEFINED_FAILURE(false);
		
		private boolean success;
		
		AuthenticationStatus(boolean success) {
			this.success = success;
		}
		public boolean isSuccess() {
			return success;
		}
	}
	
	public static void addConfig(Config config) {
		if (config == null)
			throw new IllegalArgumentException("Config can not be null");
		LdapKit.config = config;
	}
	
	public static Config getConfig() {
		return config;
	}
	
	public static DirContext getContext(){
		return getConfig().getCtx();
	}
	
	public static LdapContext getLdapContext(){
		return getConfig().getLdapCtx();
	}
	
	public static void closeContext(DirContext context) {
		if (context != null) {
			try {
				context.close();
			}
			catch (NamingException ex) {
				LOGGER.debug("Could not close JNDI DirContext", ex);
			}
			catch (Throwable ex) {
				LOGGER.debug("Unexpected exception on closing JNDI DirContext", ex);
			}
		}
	}

	public static Class getActualTargetClass(DirContext context) {
		if (context instanceof LdapContext) {
			return LdapContext.class;
		}

		return DirContext.class;
	}

	public static void collectAttributeValues(Attributes attributes, String name, Collection<Object> collection) {
        collectAttributeValues(attributes, name, collection, Object.class);
	}

    public static <T> void collectAttributeValues(
            Attributes attributes, String name, Collection<T> collection, Class<T> clazz) {
        Attribute attribute = attributes.get(name);
        if (attribute == null) {
        	throw new LdapPluginException("No attribute with name '" + name + "'");
        }
        iterateAttributeValues(attribute, new CollectingAttributeValueCallbackHandler<T>(collection, clazz));
    }
    
	public static void iterateAttributeValues(Attribute attribute, CollectingAttributeValueCallbackHandler callbackHandler) {
		for (int i = 0; i < attribute.size(); i++) {
			try {
				callbackHandler.handleAttributeValue(attribute.getID(), attribute.get(i), i);
			}
			catch (javax.naming.NamingException e) {
				throw new LdapPluginException(e);
			}
		}
	}

	private static final class CollectingAttributeValueCallbackHandler<T>{
		private final Collection<T> collection;
        private final Class<T> clazz;

        public CollectingAttributeValueCallbackHandler(Collection<T> collection, Class<T> clazz) {
			this.collection = collection;
            this.clazz = clazz;
		}

		public void handleAttributeValue(String attributeName, Object attributeValue, int index) {
			collection.add(clazz.cast(attributeValue));
		}
	}

	public static String convertCompositeNameToString(CompositeName compositeName) {
		if (compositeName.size() > 0) {
			return compositeName.get(0);
		}
		else {
			return "";
		}
	}

    public static LdapName newLdapName(Name name) {
        if(name instanceof LdapName) {
            return (LdapName) name.clone();
        } else if (name instanceof CompositeName) {
            CompositeName compositeName = (CompositeName) name;
            try {
                return new LdapName(convertCompositeNameToString(compositeName));
            } catch (InvalidNameException e) {
            	throw new LdapPluginException(e);
            }
        } else {
            LdapName result = emptyLdapName();
            try {
                result.addAll(0, name);
            } catch (InvalidNameException e) {
            	throw new LdapPluginException(e);
            }
            return result;
        }
    }

    public static LdapName newLdapName(String distinguishedName) {
        try {
            return new LdapName(distinguishedName);
        } catch (InvalidNameException e) {
        	throw new LdapPluginException(e);
        }
    }


    private static LdapName returnOrConstructLdapNameFromName(Name name) {
        if (name instanceof LdapName) {
            return (LdapName) name;
        } else {
            return newLdapName(name);
        }
    }

    public static LdapName removeFirst(Name dn, Name pathToRemove) {
        LdapName result = newLdapName(dn);
        LdapName path = returnOrConstructLdapNameFromName(pathToRemove);

        if(path.size() == 0 || !dn.startsWith(path)) {
            return result;
        }

        for(int i = 0; i < path.size(); i++) {
            try {
                result.remove(0);
            } catch (InvalidNameException e) {
            	throw new LdapPluginException(e);
            }
        }
        return result;
    }

    public static LdapName prepend(Name dn, Name pathToPrepend) {
        LdapName result = newLdapName(dn);
        try {
            result.addAll(0, pathToPrepend);
        } catch (InvalidNameException e) {
        	throw new LdapPluginException(e);
        }
        return result;
    }

    public static LdapName emptyLdapName() {
        return newLdapName("");
    }

    public static Rdn getRdn(Name name, String key) {
        LdapName ldapName = returnOrConstructLdapNameFromName(name);
        List<Rdn> rdns = ldapName.getRdns();
        for (Rdn rdn : rdns) {
            NamingEnumeration<String> ids = rdn.toAttributes().getIDs();
            while (ids.hasMoreElements()) {
                String id = ids.nextElement();
                if(key.equalsIgnoreCase(id)) {
                    return rdn;
                }
            }
        }
        throw new NoSuchElementException("No Rdn with the requested key: '" + key + "'");
    }

    public static Object getValue(Name name, String key) {
        NamingEnumeration<? extends Attribute> allAttributes = getRdn(name, key).toAttributes().getAll();
        while (allAttributes.hasMoreElements()) {
            Attribute oneAttribute = allAttributes.nextElement();
            if(key.equalsIgnoreCase(oneAttribute.getID())) {
                try {
                    return oneAttribute.get();
                } catch (javax.naming.NamingException e) {
                	throw new LdapPluginException(e);
                }
            }
        }
        // This really shouldn't happen
        throw new NoSuchElementException("No Rdn with the requested key: '" + key + "'");
    }

    public static Object getValue(Name name, int index) {
        LdapName ldapName = returnOrConstructLdapNameFromName(name);
        Rdn rdn = ldapName.getRdn(index);
        if(rdn.size() > 1) {
            LOGGER.warn("Rdn at position " + index + " of dn '" + name +
                    "' is multi-value - returned value is not to be trusted. " +
                    "Consider using name-based getValue method instead");
        }
        return rdn.getValue();
    }

    public static String getStringValue(Name name, int index) {
        return (String) getValue(name, index);
    }

    public static String getStringValue(Name name, String key) {
        return (String) getValue(name, key);
    }
    
	public static void closeNamingEnumeration(NamingEnumeration<?> enumeration) {
		try {
			if (enumeration != null) {
				enumeration.close();
			}
		}
		catch (javax.naming.NamingException e) {
			// Never mind this
		}
	}
	
	public static void modifyAttributes(DirContext context, String name, ModificationItem[] mods)
		    throws Exception{
		try {
			context.modifyAttributes(name, mods);
		} catch (javax.naming.NamingException ex) {
			throw new LdapPluginException(ex);
		}finally{
			LdapKit.closeContext(context);
		}
	}
	
	public static void modifyAttributes(String name, ModificationItem[] mods)
		    throws Exception{
		modifyAttributes(LdapKit.newLdapName(name), mods);
	}
	
	public static void modifyAttributes(Name name, ModificationItem[] mods){
		DirContext context = getContext();
		try {
			context.modifyAttributes(name, mods);
		} catch (javax.naming.NamingException ex) {
			throw new LdapPluginException(ex);
		}finally{
			LdapKit.closeContext(context);
		}
	}
	
	public static List<Entry> find(Name baseDN, String filter, Object[] filterArgs, SearchControls cons){
		List<Entry> result = new ArrayList<Entry>();
		DirContext ctx = null;
		NamingEnumeration<SearchResult> namingResults = null;
		try{
			ctx = getContext();
			namingResults = ctx.search(baseDN, filter, filterArgs, cons);
			result = EntryBuilder.build(namingResults);
		}catch(Exception e){
			throw new LdapPluginException(e);
		}finally{
			LdapKit.closeContext(ctx);
			LdapKit.closeNamingEnumeration(namingResults);
		}
		return result;
	}
	
	public static List<Entry> find(String baseDN, String filter, Object[] filterArgs, SearchControls cons){
		return find(LdapKit.newLdapName(baseDN), filter, filterArgs, cons);
	}
	
	public static List<Entry> find(String baseDN, String filter, SearchControls cons){
		return find(baseDN, filter, null, cons);
	}
	
	public static List<Entry> find(String baseDN, String filter){
		return find(baseDN, filter, SearchScope.ONELEVEL.searchControls());
	}
	
	public static boolean authenticate(String base, String filter, String password)throws Exception {
		return authenticate(LdapKit.newLdapName(base), filter, 
				password, SearchScope.ONELEVEL.searchControls()).isSuccess();
	}
	
	private static AuthenticationStatus authenticate(Name base,String filter,
            String password, SearchControls cons)throws Exception{
		List<Entry> result = find(base, filter, null, cons);
		if (result.size() == 0) {
            String msg = "No results found for search, base: '" + base + "'; filter: '" + filter + "'.";
            LOGGER.info(msg);
            return AuthenticationStatus.EMPTYRESULT;
        } else if (result.size() != 1) {
            LOGGER.info("base: '" + base + "'; filter: '" + filter + "'. size:" + result.size());
            return AuthenticationStatus.MANYRESULT;
        }
		Config config = getConfig();
		Entry entry = result.get(0);
		DirContext ctx = null;
		try{
			ctx = config.doGetContext(entry.getFdn(), password);
			return AuthenticationStatus.SUCCESS;
		}catch(Exception e){
			LOGGER.info("Authentication failed for entry with DN '" + entry.getFdn() + "'", e);
			return AuthenticationStatus.UNDEFINED_FAILURE;
		}finally{
			LdapKit.closeContext(ctx);
		}
	}
	
	public static boolean create(DirContext context, String dn, Object obj, Attributes attrs){
		try {
			context.bind(dn, null, attrs);
			return true;
		}catch (javax.naming.NamingException ex) {
			throw new LdapPluginException(ex);
		}finally{
			LdapKit.closeContext(context);
		}
	}
	
	public static boolean create(String dn, Object obj, Attributes attrs){
		return create(getContext(), dn, obj, attrs);
	}
	
	public static boolean delete(String dn){
		DirContext context = null;
		try {
			context = getContext();
			context.unbind(dn);
			return true;
		}catch(javax.naming.NamingException ex) {
			throw new LdapPluginException(ex);
		}finally{
			LdapKit.closeContext(context);
		}
	}
	
	public static boolean rename(DirContext context, String oldName, String newName){
		try {
			context.rename(oldName, newName);
			return true;
		} catch (NamingException e) {
			throw new LdapPluginException(e);
		}finally{
			LdapKit.closeContext(context);
		}
	}
	
	public static boolean rename(String oldName, String newName){
		return rename(getContext(), oldName, newName);
	}
	
	public static Page<Entry> paginate(String base, String filter,
			int pageNumber, int pageSize){
		return paginate(base, filter, null, SearchScope.ONELEVEL.searchControls(), pageNumber, pageSize);
	}
	
	public static Page<Entry> paginate(String base, String filter, 
			Object[] filterArgs, SearchControls cons, int pageNumber, int pageSize){
		if (pageNumber < 1 || pageSize < 1)
			throw new LdapPluginException("pageNumber and pageSize must be more than 0");
		LdapContext ctx = null;
		Map<Integer, List<Entry>> pageResult = new LinkedHashMap<Integer, List<Entry>>();
		byte[] cookie = null;
		//pageNumber start with 1
	    int totalPage = 1;
	    try{
	    	ctx = getLdapContext();
	    	ctx.setRequestControls(new Control[]{
	    			new PagedResultsControl(pageSize, Control.CRITICAL)});
	    	do{
	    		List<Entry> entryList = new LinkedList<Entry>();
	    		NamingEnumeration<SearchResult> results = ctx.search(base, filter, cons);
	    		entryList = EntryBuilder.build(results);
	    		Control[] controls = ctx.getResponseControls();
	    		if (controls != null) {
	    			for (int i = 0; i < controls.length; i++) {
	    				if (controls[i] instanceof PagedResultsResponseControl) {
		                     PagedResultsResponseControl prrc = (PagedResultsResponseControl)controls[i];
		                     cookie = prrc.getCookie();
		                 }else{
		                     // Handle other response controls (if any)
		                 }
		             }
		         }
		         ctx.setRequestControls(new Control[]{ new PagedResultsControl(pageSize, cookie, Control.CRITICAL)});
		         pageResult.put(totalPage, entryList);
		         totalPage ++;
		     } while (cookie != null);
	    }catch (Exception e) {
	    	throw new LdapPluginException(e);
		}finally{
			LdapKit.closeContext(ctx);
		}
	    int totalPageSize = pageResult.keySet().size();
	    int totalResultRow = 0;
	    if(totalPageSize==1){
	    	totalResultRow = pageResult.get(1).size();
	    }else if(totalPageSize > 1){
	    	int lastResultSize = pageResult.get(totalPageSize).size();
	    	totalResultRow = pageSize * (totalPageSize -1) + lastResultSize; 
	    }
	    if(pageNumber >= totalPageSize){
			pageNumber = totalPageSize;
		}
		return new Page<Entry>(pageResult.get(pageNumber), pageNumber, pageSize, totalPage, totalResultRow);
	}
	
}