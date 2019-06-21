package hello;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Package : hello
 * --
 * Description :
 * Author : jasonlin
 * Date : 2017/1/4
 */
@Configuration
@Slf4j
public class HelloWebMvcConfigurer extends WebMvcConfigurerAdapter {
    /**
     * 日志对象
     */
    private final static Logger log = LoggerFactory
            .getLogger(HelloWebMvcConfigurer.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String downloadedContentUri = HelloApplication.downloadedContentDir.toUri().toASCIIString();
        log.info("downloaded dir : {}", downloadedContentUri);
        registry.addResourceHandler("/downloaded/**")
                .addResourceLocations(downloadedContentUri);
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
