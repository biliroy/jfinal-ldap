======================================================
JAVA 极速WEB+ORM框架 JFinal LDAP Plugin
======================================================

    JFinal的LDAP(轻量目录服务)访问拓展，常用的:用户认证、筛选、变更、删除操作等。;)

JFinal-Ldap有如下主要特点
------------------------
#. LdapKit + Entry模式，灵活便利

**以下是JFinal-Ldap的示例：**

**1. 配置(JFinalConfig) **

:: 
 
    public void configPlugin(Plugins me) {
		
	LdapPlugin ldapPlugin = new LdapPlugin(getProperty("ldap.url"),
				getProperty("ldap.base"),getProperty("ldap.userDn"),
				getProperty("ldap.principal"));
	me.add(ldapPlugin);
    }

**2. 配置 Lable **
:: 
  
     public class Person extends Lable<Person>{
	
	@Override
	public String baseDN() {
		return null;
	}

	@Override
	public String rdnKey() {
		return null;
	}

	@Override
	public String[] searchAttributes() {
		return null;
	}

	@Override
	public String[] requiredAttributes() {
		return null;
	}

	@Override
	public String[] buildObjectClasses() {
		return null;
	}

	@Override
	public SearchControls searchControls() {
		return null;
	}
     ｝


** 未解决 jstl取值，需重写 Render 或组织 Controller
