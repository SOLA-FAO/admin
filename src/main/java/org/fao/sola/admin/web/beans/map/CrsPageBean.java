package org.fao.sola.admin.web.beans.map;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.helpers.MessageBean;
import org.fao.sola.admin.web.beans.helpers.MessageProvider;
import org.sola.services.common.EntityAction;
import org.sola.services.ejb.system.businesslogic.SystemEJBLocal;
import org.sola.services.ejb.system.repository.entities.Crs;

/**
 * Contains methods and properties to manage {@link Crs}
 */
@Named
@ViewScoped
public class CrsPageBean extends AbstractBackingBean {
    private Crs crs;
    private List<Crs> crsList;
    
    @Inject
    MessageBean msg;

    @Inject
    MessageProvider msgProvider;

    @EJB
    SystemEJBLocal systemEjb;

    public Crs getCrs() {
        return crs;
    }

    public List<Crs> getCrsList() {
        return crsList;
    }
    
    @PostConstruct
    private void init(){
        loadList();
    }
    
    private void loadList(){
        crsList = systemEjb.getCrss();
    }
    
    public void loadCrs(Integer srid) {
        if (srid == null) {
            crs = new Crs();
            crs.setSrid(0);
        } else {
            crs = systemEjb.getCrs(srid);
        }
    }
    
    public void deleteCrs(Crs crs) {
        crs.setEntityAction(EntityAction.DELETE);
        systemEjb.saveCrs(crs);
        loadList();
    }

    public void saveCrs() throws Exception {
        if (crs != null) {
            systemEjb.saveCrs(crs);
            loadList();
        }
    }
}
