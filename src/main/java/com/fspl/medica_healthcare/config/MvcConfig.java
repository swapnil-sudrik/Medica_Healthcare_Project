package com.fspl.medica_healthcare.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

  //	@Autowired
  //	RoleToUserProfileConverter roleToUserProfileConverter;

  /**
   * Configure ResourceHandlers to serve static resources like CSS/ Javascript etc...
   * https://www.baeldung.com/spring-mvc-static-resources
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/static/**").addResourceLocations("/static/");
  }

  @Bean
  public SpringResourceTemplateResolver templateResolver() {
    // SpringResourceTemplateResolver automatically integrates with Spring's own
    // resource resolution infrastructure, which is highly recommended.
    SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
    //        templateResolver.setApplicationContext(this.applicationContext);
    templateResolver.setPrefix("/WEB-INF/views/");
    templateResolver.setSuffix(".html");
    // HTML is the default value, added here for the sake of clarity.
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setTemplateMode("HTML5");
    templateResolver.setCharacterEncoding("UTF-8");
    // Template cache is true by default. Set to false if you want
    // templates to be automatically updated when modified.
    templateResolver.setCacheable(false);
    return templateResolver;
  }

  @Bean
  public SpringTemplateEngine templateEngine() {
    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(templateResolver());
    templateEngine.addDialect(new SpringSecurityDialect()); // Adds Spring Security dialect
    templateEngine.setEnableSpringELCompiler(true);
    return templateEngine;
  }

  /**
   * Configure Converter to be used. In our example, we need a converter to convert string
   * values[Roles] to UserProfiles in newUser.jsp
   */
  //    @Override
  //    public void addFormatters(FormatterRegistry registry) {
  //        registry.addConverter(roleToUserProfileConverter);
  //    }

  /**
   * * Configure MessageSource to lookup any validation/error message in internationalized property
   * files
   */
  @Bean
  public MessageSource messageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("messages");
    messageSource.setDefaultEncoding("UTF-8");

    return messageSource;
  }

}
