package org.fao.sola.admin.web.beans.helpers;

import java.io.Serializable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.fao.sola.admin.web.beans.language.LanguageBean;
import org.sola.common.StringUtility;

/**
 * Provides methods to extract messages from the bundle files
 */
@Named
@SessionScoped
public class MessageProvider implements Serializable {

    private ResourceBundle msgBundle;
    private ResourceBundle errBundle;
    private final String MESSAGE_BUNDLE = "messages";
    private final String ERROR_BUNDLE = "errors";
    private static final String OT_LANGAUGE = "ot_language";
    
    @Inject
    LanguageBean langBean;
    
    public MessageProvider(){
    }
    
    /** 
     * Returns bundle with errors messages.
     * @return  
     */
    public ResourceBundle getErrorsBundle() {
        if (errBundle == null) {
            reloadBundles();
         }
        return errBundle;
    }
    
    /** 
     * Returns bundle with general messages.
     * @return  
     */
    public ResourceBundle getMessagesBundle() {
        if (msgBundle == null) {
            reloadBundles();
        }
        return msgBundle;
    }

    /** 
     * Reload message bundles. 
     * @param localeCode Locale code to to use for loading bundles
     */
    public void reloadBundles(String localeCode){
        msgBundle = getBundle(MESSAGE_BUNDLE, localeCode);
        errBundle = getBundle(ERROR_BUNDLE, localeCode);
    }
    
    /** Reload message bundles by picking up language code from cookies. */
    public void reloadBundles(){
        msgBundle = getBundle(MESSAGE_BUNDLE, langBean.getLocale());
        errBundle = getBundle(ERROR_BUNDLE, langBean.getLocale());
    }
    
    /** 
     * Returns bundle by provided name.
     * @param localeCode Locale code
     * @param name Bundle name
     * @return  
     */
    public ResourceBundle getBundle(String name, String localeCode) {
        String langCode = localeCode;
        String countryCode = localeCode;
        if (localeCode.indexOf("-") > -1){
            langCode = localeCode.substring(0, localeCode.indexOf("-"));
            countryCode = localeCode.substring(localeCode.lastIndexOf("-") + 1, localeCode.length());
        }
        if(!StringUtility.isEmpty(localeCode)){
            return java.util.ResourceBundle.getBundle("org/fao/sola/admin/web/" + name, new Locale(langCode, countryCode)); 
        } else {
            return java.util.ResourceBundle.getBundle("org/fao/sola/admin/web/" + name, new Locale("en", "US")); 
        }
        
    }
    
    /** 
     * Returns error message value of the provided bundle key.
     * @param key Bundle key
     * @return  
     */
    public String getErrorMessage(String key) {
        String result = null;
        try {
            result = getErrorsBundle().getString(key);
        } catch (MissingResourceException e) {
            result = "???" + key + "??? not found";
        }
        return result;
    }
    
    /** 
     * Returns message value of the provided bundle key.
     * @param key Bundle key
     * @return  
     */
    public String getMessage(String key) {
        String result = null;
        try {
            result = getMessagesBundle().getString(key);
        } catch (MissingResourceException e) {
            result = "???" + key + "??? not found";
        }
        return result;
    }
}
