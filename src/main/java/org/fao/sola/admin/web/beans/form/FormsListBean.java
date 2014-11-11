package org.fao.sola.admin.web.beans.form;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.helpers.ErrorKeys;
import org.fao.sola.admin.web.beans.helpers.MessageProvider;
import org.fao.sola.admin.web.beans.language.LanguageBean;
import org.sola.opentenure.services.ejbs.claim.businesslogic.ClaimEJBLocal;
import org.sola.opentenure.services.ejbs.claim.entities.FormTemplate;
import org.sola.services.common.EntityAction;

/**
 * Contains methods to get list of available dynamic forms
 */
@Named
@ViewScoped
public class FormsListBean extends AbstractBackingBean {

    @EJB
    ClaimEJBLocal claimEjb;

    @Inject
    LanguageBean langBean;

    @Inject
    MessageProvider msgProvider;
    
    private FormTemplate[] forms;

    public FormsListBean() {
    }

    @PostConstruct
    private void init() {
        List<FormTemplate> formsList = claimEjb.getFormTemplates(null);
        if (formsList != null) {
            forms = formsList.toArray(new FormTemplate[formsList.size()]);
        }
    }

    public FormTemplate[] getForms() {
        return forms;
    }

    public boolean canBeDeleted(String formName){
        return claimEjb.checkFormTemplateHasPayload(formName);
    }
    
    public void delete(FormTemplate fTmpl) {
        try {
            if(fTmpl == null){
                return;
            }
            
            // Check for existing payload records
            if (claimEjb.checkFormTemplateHasPayload(fTmpl.getName())) {
                getContext().addMessage(null, new FacesMessage(msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FORM_HAS_PAYLOAD)));
                return;
            }
            
            fTmpl.setEntityAction(EntityAction.DELETE);
            claimEjb.saveFormTemplate(fTmpl);
            init();
        } catch (Exception e) {
            getContext().addMessage(null, new FacesMessage(processException(e, langBean.getLocale()).getMessage()));
        }
    }
}
