package com.jfinal.plugin.ldap;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfinal.plugin.ldap.filter.Filter;

@SuppressWarnings("rawtypes")
public final class LdapKit {

	private static final Logger LOGGER = LoggerFactory.getLogger(LdapKit.class);
    private static final int HEX = 16;

    private static Config config = new Config();
    
    /**
	 * Not to be instantiated.
	 */
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

	public static String convertCompositeNameToString(
			CompositeName compositeName) {
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

	/**
	 * Converts a binary SID to its String representation, according to the
	 * algorithm described <a
	 * href="http://blogs.msdn.com/oldnewthing/archive/2004/03/15/89753.aspx"
	 * >here</a>. Thanks to <a href=
	 * "http://www.jroller.com/eyallupu/entry/java_jndi_how_to_convert">Eyal
	 * Lupu</a> for algorithmic inspiration.
	 * 
	 * <pre>
	 * If you have a SID like S-a-b-c-d-e-f-g-...
	 * 
	 * Then the bytes are
	 * a	(revision)
	 * N	(number of dashes minus two)
	 * bbbbbb	(six bytes of &quot;b&quot; treated as a 48-bit number in big-endian format)
	 * cccc	(four bytes of &quot;c&quot; treated as a 32-bit number in little-endian format)
	 * dddd	(four bytes of &quot;d&quot; treated as a 32-bit number in little-endian format)
	 * eeee	(four bytes of &quot;e&quot; treated as a 32-bit number in little-endian format)
	 * ffff	(four bytes of &quot;f&quot; treated as a 32-bit number in little-endian format)
	 * etc.	
	 * 
	 * So for example, if your SID is S-1-5-21-2127521184-1604012920-1887927527-72713, then your raw hex SID is
	 * 
	 * 010500000000000515000000A065CF7E784B9B5FE77C8770091C0100
	 * 
	 * This breaks down as follows:
	 * 01	S-1
	 * 05	(seven dashes, seven minus two = 5)
	 * 000000000005	(5 = 0x000000000005, big-endian)
	 * 15000000	(21 = 0x00000015, little-endian)
	 * A065CF7E	(2127521184 = 0x7ECF65A0, little-endian)
	 * 784B9B5F	(1604012920 = 0x5F9B4B78, little-endian)
	 * E77C8770	(1887927527 = 0X70877CE7, little-endian)
	 * 091C0100	(72713 = 0x00011c09, little-endian)
	 * 
	 * S-1-	version number (SID_REVISION)
	 * -5-	SECURITY_NT_AUTHORITY
	 * -21-	SECURITY_NT_NON_UNIQUE
	 * -...-...-...-	these identify the machine that issued the SID
	 * 72713	unique user id on the machine
	 * </pre>
	 * 
	 * @param sid binary SID in byte array format
	 * @return String version of the given sid
	 */
	public static String convertBinarySidToString(byte[] sid) {
		// Add the 'S' prefix
		StringBuffer sidAsString = new StringBuffer("S-");

		// bytes[0] : in the array is the version (must be 1 but might
		// change in the future)
		sidAsString.append(sid[0]).append('-');

		// bytes[2..7] : the Authority
		StringBuffer sb = new StringBuffer();
		for (int t = 2; t <= 7; t++) {
			String hexString = Integer.toHexString(sid[t] & 0xFF);
			sb.append(hexString);
		}
		sidAsString.append(Long.parseLong(sb.toString(), HEX));

		// bytes[1] : the sub authorities count
		int count = sid[1];

		// bytes[8..end] : the sub authorities (these are Integers - notice
		// the endian)
		for (int i = 0; i < count; i++) {
			int currSubAuthOffset = i * 4;
			sb.setLength(0);
			sb.append(toHexString((byte) (sid[11 + currSubAuthOffset] & 0xFF)));
			sb.append(toHexString((byte) (sid[10 + currSubAuthOffset] & 0xFF)));
			sb.append(toHexString((byte) (sid[9 + currSubAuthOffset] & 0xFF)));
			sb.append(toHexString((byte) (sid[8 + currSubAuthOffset] & 0xFF)));

			sidAsString.append('-').append(Long.parseLong(sb.toString(), HEX));
		}

		// That's it - we have the SID
		return sidAsString.toString();
	}
	
	/**
	 * Converts a String SID to its binary representation, according to the
	 * algorithm described <a
	 * href="http://blogs.msdn.com/oldnewthing/archive/2004/03/15/89753.aspx"
	 * >here</a>. 
	 * 
	 * @param string SID in readable format
	 * @return Binary version of the given sid
	 * @see LdapKit#convertBinarySidToString(byte[])
	 * @since 1.3.1
	 */
	public static byte[] convertStringSidToBinary(String string) {
		String[] parts = string.split("-");
		byte sidRevision = (byte) Integer.parseInt(parts[1]);
		int subAuthCount = parts.length - 3;

        byte[] sid = new byte[] {sidRevision, (byte) subAuthCount};
		sid = addAll(sid, numberToBytes(parts[2], 6, true));
		for (int i = 0; i < subAuthCount; i++) {
			sid = addAll(sid, numberToBytes(parts[3 + i], 4, false));
		}
		return sid;
	}

    private static byte[] addAll(byte[] array1, byte[] array2) {
        byte[] joinedArray = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
	 * Converts the given number to a binary representation of the specified
	 * length and "endian-ness".
	 * 
	 * @param number String with number to convert
	 * @param length How long the resulting binary array should be
	 * @param bigEndian <code>true</code> if big endian (5=0005), or
	 * <code>false</code> if little endian (5=5000)
	 * @return byte array containing the binary result in the given order
	 */
	static byte[] numberToBytes(String number, int length, boolean bigEndian) {
		BigInteger bi = new BigInteger(number);
		byte[] bytes = bi.toByteArray();
		int remaining = length - bytes.length;
		if (remaining < 0) {
			bytes = Arrays.copyOfRange(bytes, -remaining, bytes.length);
		} else {
			byte[] fill = new byte[remaining];
			bytes = addAll(fill, bytes);
		}
		if (!bigEndian) {
			reverse(bytes);
		}
		return bytes;
	}

    private static void reverse(byte[] array) {
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
	 * Converts a byte into its hexadecimal representation, padding with a
	 * leading zero to get an even number of characters.
	 * 
	 * @param b value to convert
	 * @return hex string, possibly padded with a zero
	 */
	static String toHexString(final byte b) {
		String hexString = Integer.toHexString(b & 0xFF);
		if (hexString.length() % 2 != 0) {
			// Pad with 0
			hexString = "0" + hexString;
		}
		return hexString;
	}

	/**
	 * Converts a byte array into its hexadecimal representation, padding each
	 * with a leading zero to get an even number of characters.
	 * 
	 * @param b values to convert
	 * @return hex string, possibly with elements padded with a zero
	 */
	public static String toHexString(final byte[] b) {
		StringBuffer sb = new StringBuffer("{");
		for (int i = 0; i < b.length; i++) {
			sb.append(toHexString(b[i]));
			if (i < b.length - 1) {
				sb.append(",");
			}
		}
		sb.append("}");
		return sb.toString();
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
	
	public static List<Entry> find(String baseDN, Filter filter, Object[] filterArgs, SearchControls cons){
		return find(LdapKit.newLdapName(baseDN), filter.encode(), filterArgs, cons);
	}
	
	public static List<Entry> find(String baseDN, Filter filter, SearchControls cons){
		return find(baseDN, filter, null, cons);
	}
	
	public static List<Entry> find(String baseDN, Filter filter){
		return find(baseDN, filter, SearchScope.ONELEVEL.searchControls());
	}
	
	public static boolean authenticate(String base, String filter, String password) {
		return authenticate(LdapKit.newLdapName(base), filter, password, SearchScope.ONELEVEL.searchControls()).isSuccess();
	}
	
	private static AuthenticationStatus authenticate(Name base,String filter,
            String password, SearchControls cons){
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
	
	public boolean create(DirContext context, String dn, Object obj, Attributes attrs){
		try {
			context.bind(dn, null, attrs);
			return true;
		}catch (javax.naming.NamingException ex) {
			throw new LdapPluginException(ex);
		}finally{
			LdapKit.closeContext(context);
		}
	}
	
	public boolean create(String dn, Object obj, Attributes attrs){
		return create(getContext(), dn, obj, attrs);
	}
	
	public boolean delete(String dn){
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
	
	public boolean rename(DirContext context, String oldName, String newName){
		try {
			context.rename(oldName, newName);
			return true;
		} catch (NamingException e) {
			throw new LdapPluginException(e);
		}finally{
			LdapKit.closeContext(context);
		}
	}
	
	public boolean rename(String oldName, String newName){
		return rename(getContext(), oldName, newName);
	}
	
	
}