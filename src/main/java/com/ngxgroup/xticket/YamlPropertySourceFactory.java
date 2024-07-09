package com.ngxgroup.xticket;

import java.io.IOException;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

/**
 *
 * @author bokon
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    public org.springframework.core.env.PropertySource<?> createPropertySource(String string, EncodedResource encodedResource) throws IOException {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(encodedResource.getResource());

        return new PropertiesPropertySource(encodedResource.getResource().getFilename(), factory.getObject());
    }
}
