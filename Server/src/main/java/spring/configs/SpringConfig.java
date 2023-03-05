package spring.configs;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import spring.beans.repositories.GroupRepository;
import spring.beans.repositories.UserRepository;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Configuration
@EnableJpaRepositories(basePackageClasses = {UserRepository.class, GroupRepository.class})
@ComponentScan("spring.beans")
@PropertySource(value = "application.properties")
@EnableTransactionManagement

public class SpringConfig {
    //идея написания данного класса конфигурации была взята с https://www.petrikainulainen.net/programming/spring-framework/spring-data-jpa-tutorial-part-one-configuration/
    @Bean
    BCryptPasswordEncoder getBcryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }
    @Bean(destroyMethod = "close")
    DataSource dataSource(Environment env) {
        HikariConfig dataSourceConfig = new HikariConfig();
        dataSourceConfig.setDriverClassName(env.getRequiredProperty("db.driver"));
        dataSourceConfig.setJdbcUrl(env.getRequiredProperty("db.url"));
        dataSourceConfig.setUsername(env.getRequiredProperty("db.username"));
        dataSourceConfig.setPassword(env.getRequiredProperty("db.password"));
        return new HikariDataSource(dataSourceConfig);
    }
    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, Environment env) {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource);
        entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        entityManagerFactoryBean.setPackagesToScan("entities");
        Properties jpaProperties = new Properties();
        Consumer<String> extractAndPutProperty = (property)->{
            jpaProperties.put(property, env.getRequiredProperty(property));
        };
        MutablePropertySources propSrcs = ((AbstractEnvironment) env).getPropertySources();
        Predicate<String> checkNameForHibernateProperty = Pattern.compile("hibernate\\.\\S+").asPredicate();
        StreamSupport.stream(propSrcs.spliterator(), false)
                .filter(propertySource -> checkNameForHibernateProperty.test(propertySource.getName()))
                .forEach(propertySource -> extractAndPutProperty.accept(propertySource.getName()));
        entityManagerFactoryBean.setJpaProperties(jpaProperties);
        return entityManagerFactoryBean;
    }
    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
}