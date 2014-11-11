package org.fao.sola.admin.web.beans.log;

import java.util.List;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.helpers.MessageBean;
import org.fao.sola.admin.web.beans.helpers.MessageProvider;
import org.fao.sola.admin.web.beans.language.LanguageBean;
import org.sola.services.common.EntityAction;
import org.sola.services.ejb.system.businesslogic.SystemEJBLocal;
import org.sola.services.ejb.system.repository.entities.EmailTask;

/**
 * Contains methods to manage mail messages queue
 */
@Named
@ViewScoped
public class MailQueuePageBean extends AbstractBackingBean {

    @EJB
    SystemEJBLocal systemEjb;

    @Inject
    MessageProvider msgProvider;

    @Inject
    LanguageBean langBean;

    @Inject
    MessageBean msgBean;

    private EmailTask[] emails;

    public EmailTask[] getEmails() {
        return emails;
    }

    public void init() {
        refresh();
    }

    public void refresh() {
        List<EmailTask> emailsList = systemEjb.getEmails();
        if (emailsList != null) {
            emails = emailsList.toArray(new EmailTask[emailsList.size()]);
        }
    }

    public void deleteEmail(EmailTask email) {
        try {
            if (email == null) {
                return;
            }
            email.setEntityAction(EntityAction.DELETE);
            systemEjb.saveEmailTask(email);
            refresh();
        } catch (Exception e) {
            msgBean.setErrorMessage(e.getMessage());
        }
    }
}
