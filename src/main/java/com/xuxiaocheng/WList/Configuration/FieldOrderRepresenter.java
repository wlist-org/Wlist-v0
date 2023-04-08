package com.xuxiaocheng.WList.Configuration;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.representer.Representer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FieldOrderRepresenter extends Representer {
    public FieldOrderRepresenter() {
        super(new DumperOptions());
        super.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        super.setPropertyUtils(new PropertyUtils() {
            @Override
            protected Set<Property> createPropertySet(final Class<?> type, final BeanAccess bAccess) {
                return this.getPropertiesMap(type, BeanAccess.FIELD).values().stream()
                        .filter(prop -> prop.isReadable() && (this.isAllowReadOnlyProperties() || prop.isWritable()))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }
        });
    }
}
