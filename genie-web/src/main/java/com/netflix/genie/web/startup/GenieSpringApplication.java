/*
 *
 *  Copyright 2015 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.genie.web.startup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Genie Spring Configuration class.
 *
 * @author tgianos
 */
@Configuration
@ComponentScan("com.netflix.genie")
@EnableAutoConfiguration
@EnableElasticsearchRepositories("com.netflix.genie.server.repository.elasticsearch")
@EnableJpaRepositories("com.netflix.genie.server.repository.jpa")
@EntityScan("com.netflix.genie.common.model")
@EnableTransactionManagement
@EnableRetry
public class GenieSpringApplication extends SpringBootServletInitializer {

    /**
     * {@inheritDoc}
     */
    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder applicationBuilder) {
        return applicationBuilder.sources(GenieSpringApplication.class);
    }

    /**
     * Spring Boot Main.
     *
     * @param args Program arguments
     * @throws Exception For any failure during program execution
     */
    public static void main(final String[] args) throws Exception {
        SpringApplication.run(GenieSpringApplication.class, args);
    }
}