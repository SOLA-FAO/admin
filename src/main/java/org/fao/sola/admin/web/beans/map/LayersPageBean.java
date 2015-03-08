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
import org.sola.services.ejb.refdata.businesslogic.RefDataEJBLocal;
import org.sola.services.ejb.refdata.entities.MapLayerType;
import org.sola.services.ejb.system.businesslogic.SystemEJBLocal;
import org.sola.services.ejb.system.repository.entities.ConfigMapLayer;
import org.sola.services.ejb.system.repository.entities.ConfigMapLayerMetadata;
import org.sola.services.ejb.system.repository.entities.Query;

/**
 * Contains methods and properties to manage {@link ConfigMapLayer}
 */
@Named
@ViewScoped
public class LayersPageBean extends AbstractBackingBean {
    private ConfigMapLayer layer;
    private List<ConfigMapLayer> layers;
    private ConfigMapLayerMetadata layerMetadata;
    private LocalizedValuesListBean localizedTitleValues;
    private List<MapLayerType> layerTypes;
    private List<Query> queries;

    @Inject
    private LanguageBean languageBean;
    
    @Inject
    private MessageProvider msgProvider;

    @EJB
    private SystemEJBLocal systemEjb;
    
    @EJB
    private RefDataEJBLocal refEjb;

    public ConfigMapLayer getLayer() {
        return layer;
    }

    public List<ConfigMapLayer> getLayers() {
        return layers;
    }

    public ConfigMapLayerMetadata getLayerMetadata() {
        return layerMetadata;
    }

    public LocalizedValuesListBean getLocalizedTitleValues() {
        return localizedTitleValues;
    }

    public MapLayerType[] getLayerTypes() {
        if(layerTypes != null){
            return layerTypes.toArray(new MapLayerType[layerTypes.size()]);
        } else {
            return new MapLayerType[]{};
        }
    }

    public Query[] getQueries() {
        if(queries != null){
            return queries.toArray(new Query[queries.size()]);
        } else {
            return new Query[]{};
        }
    }

    public String getLayerTypeDisplayValue(String code){
        if(layerTypes != null){
            for(MapLayerType layerType : layerTypes){
                if(layerType.getCode().equalsIgnoreCase(code)){
                    return layerType.getDisplayValue();
                }
            }
        }
        return "";
    }
    
    @PostConstruct
    private void init(){
        loadList();
        if(layerTypes == null){
            layerTypes = new ArrayList<MapLayerType>();
            MappingManager.getMapper().map(refEjb.getCodeEntityList(MapLayerType.class, languageBean.getLocale()), layerTypes);
            MapLayerType layerType = new MapLayerType();
            layerType.setCode("");
            layerType.setDisplayValue("");
            layerTypes.add(0, layerType);
        }
    }
    
    private void loadList() {
        layers = systemEjb.getConfigMapLayers(null);
    }
    
    private void loadQueries(){
        queries = systemEjb.getQueries(languageBean.getLocale());
        Query dummyQuery = new Query();
        dummyQuery.setName("");
        queries.add(0, dummyQuery);
    }
    
    public ConfigMapLayerMetadata[] getFilteredMetadata(){
        if(layer != null && layer.getMetadataList()!= null){
            ArrayList<ConfigMapLayerMetadata> metaList = new ArrayList<>();
            for(ConfigMapLayerMetadata m : layer.getMetadataList()){
                if(m.getEntityAction() == null || m.getEntityAction() != EntityAction.DELETE){
                    metaList.add(m);
                }
            }
            return metaList.toArray(new ConfigMapLayerMetadata[metaList.size()]);
        } else {
            return new ConfigMapLayerMetadata[]{};
        }
    }
    
    public void loadMetadata(ConfigMapLayerMetadata m) {
        if (layer.getMetadataList() == null) {
            layer.setMetadataList(new ArrayList<ConfigMapLayerMetadata>());
        }
        
        if (m != null) {
            layerMetadata = MappingManager.getMapper().map(m, ConfigMapLayerMetadata.class);
        } else {
            layerMetadata = new ConfigMapLayerMetadata();
            layerMetadata.setName("");
        }
    }

    public void deleteMetadata(ConfigMapLayerMetadata m) {
        m.setEntityAction(EntityAction.DELETE);
    }

    public void saveMetadata() throws Exception {
        if (layerMetadata != null) {
            // Validate
            String errors = "";
            if (StringUtility.isEmpty(layerMetadata.getName())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.LAYERS_PAGE_FILL_NAME) + "\r\n";
            }
            if (StringUtility.isEmpty(layerMetadata.getValue())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.LAYERS_PAGE_FILL_VALUE) + "\r\n";
            }
            
            if (!errors.equals("")) {
                throw new Exception(errors);
            }

            // Search for metadata
            boolean found = false;
            for (ConfigMapLayerMetadata m : layer.getMetadataList()) {
                if (m.getName().equalsIgnoreCase(layerMetadata.getName())) {
                    // Metadata found make update
                    MappingManager.getMapper().map(layerMetadata, m);
                    found = true;
                    break;
                }
            }
            if(!found){
                // add into the list
                layer.getMetadataList().add(layerMetadata);
            }
        }
    }
    
    public void loadLayer(String name) {
        if(queries == null){
            loadQueries();
        }
        
        if (StringUtility.isEmpty(name)) {
            layer = new ConfigMapLayer();
            layer.setMetadataList(new ArrayList<ConfigMapLayerMetadata>());
            layer.setName("");
            layer.setActive(true);
        } else {
            layer = systemEjb.getConfigMapLayer(name, null);
        }
        localizedTitleValues = new LocalizedValuesListBean(languageBean);
        localizedTitleValues.loadLocalizedValues(layer.getTitle());
    }

    public void deleteLayer(ConfigMapLayer l) {
        l.setEntityAction(EntityAction.DELETE);
        systemEjb.saveConfigMapLayer(l);
        loadList();
    }

    public void saveLayer() throws Exception {
        if (layer != null) {
            // Validate
            String errors = "";
            if (StringUtility.isEmpty(layer.getName())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.LAYERS_PAGE_FILL_NAME) + "\r\n";
            }
            if (localizedTitleValues.getLocalizedValues() == null || localizedTitleValues.getLocalizedValues().size() < 1
                    || StringUtility.isEmpty(localizedTitleValues.getLocalizedValues().get(0).getLocalizedValue())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.LAYERS_PAGE_FILL_TITLE) + "\r\n";
            }
            if (StringUtility.isEmpty(layer.getTypeCode())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.LAYERS_PAGE_SELECT_TYPE) + "\r\n";
            }
            if (!StringUtility.isEmpty(layer.getTypeCode()) && layer.getTypeCode().equalsIgnoreCase("wms") && StringUtility.isEmpty(layer.getWmsLayers())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.LAYERS_PAGE_FILL_WMS_LAYERS) + "\r\n";
            }
            if (!StringUtility.isEmpty(layer.getTypeCode()) && layer.getTypeCode().equalsIgnoreCase("pojo") 
                    && StringUtility.isEmpty(layer.getPojoStructure())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.LAYERS_PAGE_FILL_POJO_STRUCTURE) + "\r\n";
            }
            if (!StringUtility.isEmpty(layer.getTypeCode()) && layer.getTypeCode().equalsIgnoreCase("pojo") 
                    && StringUtility.isEmpty(layer.getPojoQueryName())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.LAYERS_PAGE_FILL_POJO_QUERY) + "\r\n";
            }
            if (!StringUtility.isEmpty(layer.getTypeCode()) && layer.getTypeCode().equalsIgnoreCase("pojo") 
                    && StringUtility.isEmpty(layer.getStyle())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.LAYERS_PAGE_FILL_STYLE) + "\r\n";
            }
            if (!StringUtility.isEmpty(layer.getTypeCode()) && layer.getTypeCode().equalsIgnoreCase("shape") 
                    && StringUtility.isEmpty(layer.getShapeLocation())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.LAYERS_PAGE_FILL_SHAPE_LOCATION) + "\r\n";
            }
            if (!StringUtility.isEmpty(layer.getTypeCode()) && layer.getTypeCode().equalsIgnoreCase("shape") 
                    && StringUtility.isEmpty(layer.getStyle())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.LAYERS_PAGE_FILL_STYLE) + "\r\n";
            }

            if (!errors.equals("")) {
                throw new Exception(errors);
            }

            layer.setTitle(localizedTitleValues.buildMultilingualString());
            
            // Make sure all metadata have layer name
            if(layer.getMetadataList() != null){
                for (ConfigMapLayerMetadata m : layer.getMetadataList()) {
                    if(StringUtility.isEmpty(m.getNameLayer())){
                        m.setNameLayer(layer.getName());
                    }
                }
            }
            
            systemEjb.saveConfigMapLayer(layer);
            loadList();
        }
    }
}
