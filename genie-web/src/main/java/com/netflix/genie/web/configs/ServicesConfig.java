/*
 *
 *  Copyright 2016 Netflix, Inc.
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
package com.netflix.genie.web.configs;

import com.netflix.genie.common.exceptions.GenieException;
import com.netflix.genie.core.jobs.workflow.WorkflowTask;
import com.netflix.genie.core.jpa.repositories.JpaApplicationRepository;
import com.netflix.genie.core.jpa.repositories.JpaClusterRepository;
import com.netflix.genie.core.jpa.repositories.JpaCommandRepository;
import com.netflix.genie.core.jpa.repositories.JpaJobExecutionRepository;
import com.netflix.genie.core.jpa.repositories.JpaJobRepository;
import com.netflix.genie.core.jpa.repositories.JpaJobRequestRepository;
import com.netflix.genie.core.jpa.services.JpaApplicationServiceImpl;
import com.netflix.genie.core.jpa.services.JpaClusterServiceImpl;
import com.netflix.genie.core.jpa.services.JpaCommandServiceImpl;
import com.netflix.genie.core.jpa.services.JpaJobPersistenceServiceImpl;
import com.netflix.genie.core.jpa.services.JpaJobSearchServiceImpl;
import com.netflix.genie.core.metrics.GenieNodeStatistics;
import com.netflix.genie.core.metrics.impl.GenieNodeStatisticsImpl;
import com.netflix.genie.core.services.ApplicationService;
import com.netflix.genie.core.services.ClusterLoadBalancer;
import com.netflix.genie.core.services.ClusterService;
import com.netflix.genie.core.services.CommandService;
import com.netflix.genie.core.services.FileTransfer;
import com.netflix.genie.core.services.JobCoordinatorService;
import com.netflix.genie.core.services.JobKillService;
import com.netflix.genie.core.services.JobPersistenceService;
import com.netflix.genie.core.services.JobSearchService;
import com.netflix.genie.core.services.JobSubmitterService;
import com.netflix.genie.core.services.MailService;
import com.netflix.genie.core.services.impl.DefaultMailServiceImpl;
import com.netflix.genie.core.services.impl.GenieFileTransferService;
import com.netflix.genie.core.services.impl.LocalJobKillServiceImpl;
import com.netflix.genie.core.services.impl.LocalJobRunner;
import com.netflix.genie.core.services.impl.MailServiceImpl;
import com.netflix.genie.core.services.impl.RandomizedClusterLoadBalancerImpl;
import org.apache.commons.exec.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

/**
 * Configuration for all the services.
 *
 * @author amsharma
 * @since 3.0.0
 */
@Configuration
public class ServicesConfig {

    /**
     * Returns a bean for mail service impl using the Spring Mail.
     *
     * @param javaMailSender An implementation of the JavaMailSender interface.
     * @param fromAddress    The from email address for the email.
     * @param mailUser       The userid of the account used to send email.
     * @param mailPassword   The password of the account used to send email.
     * @return An instance of MailService implementation.
     * @throws GenieException If there is any problem.
     */
    @Bean
    @ConditionalOnProperty("spring.mail.host")
    public MailService getJavaMailSenderMailService(
        final JavaMailSender javaMailSender,
        @Value("${genie.mail.fromAddress}") final String fromAddress,
        @Value("${genie.mail.user:#{null}}") final String mailUser,
        @Value("${genie.mail.password:#{null}}") final String mailPassword
    ) throws GenieException {
        return new MailServiceImpl(
            javaMailSender,
            fromAddress,
            mailUser,
            mailPassword
        );
    }

    /**
     * Get an default implementation of the Mail Service interface if nothing is supplied.
     *
     * @return The mail service implementation that does nothing.
     */
    @Bean
    @ConditionalOnMissingBean
    public MailService getDefaultMailServiceImpl() {
        return new DefaultMailServiceImpl();
    }

    /**
     * Get an implementation of the GenieNodeStatistics interface for metrics collection.
     *
     * @return A genie node statics instance.
     */
    @Bean
    public GenieNodeStatistics getGenieNodeStatistics() {
        return new GenieNodeStatisticsImpl();
    }

    /**
     * Get JPA based implementation of the ApplicationService.
     *
     * @param applicationRepo The application repository to use.
     * @param commandRepo     The command repository to use.
     * @return An application service instance.
     */
    @Bean
    public ApplicationService applicationService(
        final JpaApplicationRepository applicationRepo,
        final JpaCommandRepository commandRepo
    ) {
        return new JpaApplicationServiceImpl(applicationRepo, commandRepo);
    }

    /**
     * Get JPA based implementation of the ClusterService.
     *
     * @param clusterRepo The cluster repository to use.
     * @param commandRepo The command repository to use.
     * @return A cluster service instance.
     */
    @Bean
    public ClusterService clusterService(
        final JpaClusterRepository clusterRepo,
        final JpaCommandRepository commandRepo
    ) {
        return new JpaClusterServiceImpl(clusterRepo, commandRepo);
    }

    /**
     * Get JPA based implementation of the CommandService.
     *
     * @param commandRepo the command repository to use
     * @param appRepo     the application repository to use
     * @param clusterRepo the cluster repository to use
     * @return A command service instance.
     */
    @Bean
    public CommandService commandService(
        final JpaCommandRepository commandRepo,
        final JpaApplicationRepository appRepo,
        final JpaClusterRepository clusterRepo
    ) {
        return new JpaCommandServiceImpl(commandRepo, appRepo, clusterRepo);
    }

    /**
     * Get JPA based implementation of the JobSearchService.
     *
     * @param jobRepository          The repository to use for job entities
     * @param jobRequestRepository   The repository to use for job request entities
     * @param jobExecutionRepository The repository to use for job execution entities
     * @return A job search service instance.
     */
    @Bean
    public JobSearchService jobSearchService(
        final JpaJobRepository jobRepository,
        final JpaJobRequestRepository jobRequestRepository,
        final JpaJobExecutionRepository jobExecutionRepository
    ) {
        return new JpaJobSearchServiceImpl(jobRepository, jobRequestRepository, jobExecutionRepository);
    }

    /**
     * Get JPA based implementation of the JobPersistenceService.
     *
     * @param jobRepo          The job repository to use
     * @param jobRequestRepo   The job request repository to use
     * @param jobExecutionRepo The jobExecution Repository to use
     * @param applicationRepo  The application repository to use
     * @param clusterRepo      The cluster repository to use
     * @param commandRepo      The command repository to use
     * @return A job search service instance.
     */
    @Bean
    public JobPersistenceService jobPersistenceService(
        final JpaJobRepository jobRepo,
        final JpaJobRequestRepository jobRequestRepo,
        final JpaJobExecutionRepository jobExecutionRepo,
        final JpaApplicationRepository applicationRepo,
        final JpaClusterRepository clusterRepo,
        final JpaCommandRepository commandRepo
    ) {
        return new JpaJobPersistenceServiceImpl(
            jobRepo,
            jobRequestRepo,
            jobExecutionRepo,
            applicationRepo,
            clusterRepo,
            commandRepo
        );
    }

    /**
     * Get an local implementation of the JobKillService.
     *
     * @param hostName         The name of the host this Genie node is running on.
     * @param jobSearchService The job search service to use to locate job information.
     * @param executor         The executor to use to run system processes.
     * @param runAsUser        Whether jobs on this instance are run as the user or not
     * @return A job kill service instance.
     */
    @Bean
    public JobKillService jobKillService(
        final String hostName,
        final JobSearchService jobSearchService,
        final Executor executor,
        @Value("${genie.jobs.runasuser.enabled:false}")
        final boolean runAsUser
    ) {
        return new LocalJobKillServiceImpl(hostName, jobSearchService, executor, runAsUser);
    }

    /**
     * Get a Randomized Cluster load balancer.
     *
     * @return A randomized cluster load balancer instance.
     */
    @Bean
    public ClusterLoadBalancer clusterLoadBalancer(

    ) {
        return new RandomizedClusterLoadBalancerImpl();
    }

    /**
     * Get an instance of the Genie File Transfer service.
     *
     * @param fileTransferImpls List of implementations of all fileTransfer interface
     * @return A singleton for GenieFileTransferService
     * @throws GenieException If there is any problem
     */
    @Bean
    public GenieFileTransferService genieFileTransferService(
        final List<FileTransfer> fileTransferImpls
    ) throws GenieException {
        return new GenieFileTransferService(fileTransferImpls);
    }

    /**
     * Get a implementation of the JobSubmitterService that runs jobs locally.
     *
     * @param jss                 Implementaion of the jobSearchService.
     * @param jps                 Implementation of the job persistence service.
     * @param applicationService  Implementation of application service interface.
     * @param clusterService      Implementation of cluster service interface.
     * @param commandService      Implementation of command service interface.
     * @param clusterLoadBalancer Implementation of the cluster load balancer interface.
     * @param fts                 File Transfer service.
     * @param aep                 Instance of the event publisher.
     * @param workflowTasks       List of all the workflow tasks to be executed.
     * @param genieWorkingDir     Working directory for genie where it creates jobs directories.
     * @param hostName            Hostname of this host.
     * @param maxRunningJobs      Maximum number of jobs allowed to run on this host.
     * @return An instance of the JobSubmitterService.
     */
    @Bean
    public JobSubmitterService jobSubmitterService(
        final JobSearchService jss,
        final JobPersistenceService jps,
        final ApplicationService applicationService,
        final ClusterService clusterService,
        final CommandService commandService,
        final ClusterLoadBalancer clusterLoadBalancer,
        final GenieFileTransferService fts,
        final ApplicationEventPublisher aep,
        final List<WorkflowTask> workflowTasks,
        final Resource genieWorkingDir,
        final String hostName,
        @Value("${genie.jobs.max.running:2}")
        final int maxRunningJobs
    ) {
        return new LocalJobRunner(
            jss,
            jps,
            applicationService,
            clusterService,
            commandService,
            clusterLoadBalancer,
            fts,
            aep,
            workflowTasks,
            genieWorkingDir,
            hostName,
            maxRunningJobs
        );
    }

    /**
     * Get an instance of the JobCoordinatorService.
     *
     * @param jobPersistenceService implementation of job persistence service interface
     * @param jobSubmitterService   implementation of the job submitter service
     * @param jobKillService        The job kill service to use
     * @param baseArchiveLocation   The base directory location of where the job dir should be archived
     * @return An instance of the JobCoordinatorService.
     */
    @Bean
    public JobCoordinatorService jobCoordinatorService(
        final JobPersistenceService jobPersistenceService,
        final JobSubmitterService jobSubmitterService,
        final JobKillService jobKillService,
        @Value("${genie.jobs.archive.location}")
        final String baseArchiveLocation
    ) {
        return new JobCoordinatorService(
            jobPersistenceService,
            jobSubmitterService,
            jobKillService,
            baseArchiveLocation);
    }
}
