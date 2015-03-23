package com.jfinal.ext.plugin.ldap;

import javax.naming.directory.SearchControls;

public enum SearchScope {
    /**
     * Corresponds to {@link SearchControls#OBJECT_SCOPE}
     */
    OBJECT(
    		new SearchControls(SearchControls.OBJECT_SCOPE,1,0,null,true,false)
    ),
    /**
     * Corresponds to {@link SearchControls#ONELEVEL_SCOPE}
     */
    ONELEVEL(
    		new SearchControls(SearchControls.ONELEVEL_SCOPE,0,0,null,true,false)
    ),
    /**
     * Corresponds to {@link SearchControls#SUBTREE_SCOPE}
     */
    SUBTREE(
    		new SearchControls(SearchControls.SUBTREE_SCOPE,0,0,null,true,false)
    );

    private final SearchControls searchControls;

    private SearchScope(SearchControls searchControls) {
        this.searchControls = searchControls;
    }

    public SearchControls searchControls() {
        return searchControls;
    }
}
