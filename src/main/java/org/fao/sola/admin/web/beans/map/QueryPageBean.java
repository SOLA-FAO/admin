package org.fao.sola.admin.web.beans.map;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.helpers.ErrorKeys;
import org.fao.sola.admin.web.beans.helpers.MessageProvider;
import org.fao.sola.admin.web.beans.language.LanguageBean;
import org.fao.sola.admin.web.beans.localization.LocalizedValuesListBean;
import org.sola.common.StringUtility;
import org.sola.common.mapping.MappingManager;
import org.sola.services.common.EntityAction;
import org.sola.services.ejb.system.businesslogic.SystemEJBLocal;
import org.sola.services.ejb.system.repository.entities.Query;
import org.sola.services.ejb.system.repository.entities.QueryField;

/**
 * Contains methods and properties to manage {@link Query}
 */
@Named
@ViewScoped
public class QueryPageBean extends AbstractBackingBean {

    private Query query;
    private List<Query> queryList;
    private QueryField queryField;
    LocalizedValuesListBean localizedDisplayValues;

    @Inject
    private LanguageBean languageBean;
    
    @Inject
    private MessageProvider msgProvider;

    @EJB
    private SystemEJBLocal systemEjb;

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public List<Query> getQueryList() {
        return queryList;
    }

    public void setQueryList(List<Query> queryList) {
        this.queryList = queryList;
    }

    public QueryField getQueryField() {
        return queryField;
    }

    public void setQueryField(QueryField queryField) {
        this.queryField = queryField;
    }

    public LocalizedValuesListBean getLocalizedDisplayValues() {
        return localizedDisplayValues;
    }
    
    public QueryPageBean() {
    }

    @PostConstruct
    private void init() {
        loadList();
    }

    private void loadList() {
        queryList = systemEjb.getQueries(null);
    }

    public QueryField[] getFilteredFields(){
        if(query != null && query.getFields() != null){
            ArrayList<QueryField> fields = new ArrayList<>();
            for(QueryField f : query.getFields()){
                if(f.getEntityAction() == null || f.getEntityAction() != EntityAction.DELETE){
                    fields.add(f);
                }
            }
            return fields.toArray(new QueryField[fields.size()]);
        } else {
            return new QueryField[]{};
        }
    }
    
    public void loadField(QueryField f) {
        if (query.getFields() == null) {
            query.setFields(new ArrayList<QueryField>());
        }

        if (f != null) {
            queryField = MappingManager.getMapper().map(f, QueryField.class);
        } else {
            queryField = new QueryField();
            queryField.setName("");
        }
        localizedDisplayValues = new LocalizedValuesListBean(languageBean);
        localizedDisplayValues.loadLocalizedValues(queryField.getDisplayValue());
    }

    public void deleteField(QueryField f) {
        f.setEntityAction(EntityAction.DELETE);
    }

    public void saveField() throws Exception {
        if (queryField != null) {
            // Validate
            String errors = "";
            if (StringUtility.isEmpty(queryField.getName())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.QUERY_PAGE_FILL_NAME) + "\r\n";
            }
            
            if (!errors.equals("")) {
                throw new Exception(errors);
            }

            queryField.setDisplayValue(localizedDisplayValues.buildMultilingualString());
            
            // Search for the field
            boolean found = false;
            for (QueryField f : query.getFields()) {
                if (f.getName().equalsIgnoreCase(queryField.getName())) {
                    // Field found make update
                    MappingManager.getMapper().map(queryField, f);
                    found = true;
                    break;
                }
            }
            if(!found){
                // add into the list
                query.getFields().add(queryField);
            }
        }
    }

    public void loadQuery(String name) {
        if (StringUtility.isEmpty(name)) {
            query = new Query();
            query.setFields(new ArrayList<QueryField>());
            query.setName("");
        } else {
            query = systemEjb.getQuery(name, null);
        }
    }

    public void deleteQuery(Query query) {
        query.setEntityAction(EntityAction.DELETE);
        systemEjb.saveQuery(query);
        loadList();
    }

    public void saveQuery() throws Exception {
        if (query != null) {
            // Validate
            String errors = "";
            if (StringUtility.isEmpty(query.getName())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.QUERY_PAGE_FILL_NAME) + "\r\n";
            }
            if (StringUtility.isEmpty(query.getSql())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.QUERY_PAGE_FILL_SQL) + "\r\n";
            }

            if (!errors.equals("")) {
                throw new Exception(errors);
            }

            // Make sure all fields have query name
            if(query.getFields() != null){
                for (QueryField f : query.getFields()) {
                    if(StringUtility.isEmpty(f.getQueryName())){
                        f.setQueryName(query.getName());
                    }
                }
            }
            
            systemEjb.saveQuery(query);
            loadList();
        }
    }
}
