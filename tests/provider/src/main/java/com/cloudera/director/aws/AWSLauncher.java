// (c) Copyright 2015 Cloudera, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cloudera.director.aws;

import static com.cloudera.director.aws.AWSClientConfig.AWSClientConfigurationPropertyToken;
import static com.cloudera.director.aws.ec2.EphemeralDeviceMappings.EphemeralDeviceMappingsConfigProperties.EphemeralDeviceMappingsConfigurationPropertyToken;
import static com.cloudera.director.aws.ec2.VirtualizationMappings.VirtualizationMappingsConfigProperties.VirtualizationMappingsConfigurationPropertyToken;
import static com.cloudera.director.aws.rds.RDSEndpoints.RDSEndpointsConfigProperties.RDSEndpointsConfigurationPropertyToken;

import com.cloudera.director.aws.common.ConfigFragmentWrapper;
import com.cloudera.director.aws.common.ResourceBundleLocalizationContext;
import com.cloudera.director.aws.ec2.EphemeralDeviceMappings;
import com.cloudera.director.aws.ec2.VirtualizationMappings;
import com.cloudera.director.aws.rds.RDSEndpoints;
import com.cloudera.director.spi.v1.common.http.HttpProxyParameters;
import com.cloudera.director.spi.v1.model.ConfigurationPropertyToken;
import com.cloudera.director.spi.v1.model.Configured;
import com.cloudera.director.spi.v1.model.LocalizationContext;
import com.cloudera.director.spi.v1.model.util.SimpleConfiguration;
import com.cloudera.director.spi.v1.provider.CloudProvider;
import com.cloudera.director.spi.v1.provider.CloudProviderMetadata;
import com.cloudera.director.spi.v1.provider.util.AbstractLauncher;
import com.cloudera.director.spi.v1.util.ConfigurationPropertiesUtil;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * AWS plugin launcher.
 */
public class AWSLauncher extends AbstractLauncher {

  /**
   * The cloud provider metadata.
   */
  private static final List<CloudProviderMetadata> CLOUD_PROVIDER_METADATA =
      Collections.unmodifiableList(Collections.singletonList(AWSProvider.METADATA));

  /**
   * Creates ephemeral device mappings with the specified parameters.
   *
   * @param config                   the configuration
   * @param configurationDirectory   the plugin configuration directory
   * @param cloudLocalizationContext the parent cloud localization context
   * @return the ephemeral device mappings
   */
  private static EphemeralDeviceMappings getEphemeralDeviceMappings(Config config,
      File configurationDirectory, LocalizationContext cloudLocalizationContext) {
    return new EphemeralDeviceMappings(getConfiguration(config,
        Configurations.EPHEMERAL_DEVICE_MAPPINGS_SECTION,
        EphemeralDeviceMappingsConfigurationPropertyToken.values()),
        configurationDirectory, cloudLocalizationContext);
  }

  /**
   * Creates virtualization mappings with the specified parameters.
   *
   * @param config                   the configuration
   * @param configurationDirectory   the plugin configuration directory
   * @param cloudLocalizationContext the parent cloud localization context
   * @return the virtualization mappings
   */
  private static VirtualizationMappings getVirtualizationMappings(Config config,
      File configurationDirectory, LocalizationContext cloudLocalizationContext) {
    return new VirtualizationMappings(getConfiguration(config,
        Configurations.VIRTUALIZATION_MAPPINGS_SECTION,
        VirtualizationMappingsConfigurationPropertyToken.values()),
        configurationDirectory, cloudLocalizationContext);
  }

  /**
   * Creates RDS endpoints with the specified parameters.
   *
   * @param config                   the configuration
   * @param configurationDirectory   the plugin configuration directory
   * @param cloudLocalizationContext the parent cloud localization context
   * @return the RDS endpoints
   */
  private static RDSEndpoints getRDSEndpoints(Config config,
      File configurationDirectory, LocalizationContext cloudLocalizationContext) {
    return new RDSEndpoints(getConfiguration(config,
        Configurations.RDS_ENDPOINTS_SECTION,
        RDSEndpointsConfigurationPropertyToken.values()),
        configurationDirectory, cloudLocalizationContext);
  }

  /**
   * Creates AWS client config with the specified parameters.
   *
   * @param config                   the configuration
   * @param httpProxyParameters      the HTTP proxy parameters
   * @param cloudLocalizationContext the parent cloud localization context
   * @return the AWS client config
   */
  private static AWSClientConfig getAWSClientConfig(Config config,
      HttpProxyParameters httpProxyParameters,
      LocalizationContext cloudLocalizationContext) {
    return new AWSClientConfig(getConfiguration(config, Configurations.AWS_CLIENT_SECTION,
        AWSClientConfigurationPropertyToken.values()), httpProxyParameters,
        cloudLocalizationContext);
  }

  /**
   * Creates AWS filter config with the specified parameters.
   *
   * @param config the configuration
   * @return the AWS filter config
   */
  @SuppressWarnings("PMD.UselessParentheses")
  private static AWSFilters getAWSFilterConfig(Config config) {
    return AWSFilters.getAWSFilters(
        ((config == null)
            || !config.hasPath(Configurations.AWS_FILTERS_SECTION))
            ? null
            : config.getConfig(Configurations.AWS_FILTERS_SECTION));
  }

  private static <T extends ConfigurationPropertyToken> Configured getConfiguration(Config config,
      String section, T[] configurationPropertyTokens) {
    if (config == null) {
      return new SimpleConfiguration();
    }
    if (!config.hasPath(section)) {
      return new SimpleConfiguration();
    }
    Config sectionConfig = config.getConfig(section);
    //noinspection unchecked
    return new ConfigFragmentWrapper(sectionConfig,
        ConfigurationPropertiesUtil.asConfigurationPropertyList(configurationPropertyTokens));
  }

  @VisibleForTesting
  protected EphemeralDeviceMappings ephemeralDeviceMappings;

  @VisibleForTesting
  protected VirtualizationMappings virtualizationMappings;

  @VisibleForTesting
  protected RDSEndpoints rdsEndpoints;

  @VisibleForTesting
  protected AWSClientConfig awsClientConfig;

  @VisibleForTesting
  protected AWSFilters awsFilters;

  /**
   * Creates an AWS launcher.
   */
  public AWSLauncher() {
    super(CLOUD_PROVIDER_METADATA, ResourceBundleLocalizationContext.FACTORY);
  }

  @Override
  public void initialize(File configurationDirectory, HttpProxyParameters httpProxyParameters) {
    File configFile = new File(configurationDirectory, Configurations.CONFIGURATION_FILE_NAME);
    Config config = null;
    if (configFile.canRead()) {
      config = parseConfigFile(configFile);
    }

    // Configure HTTP proxy settings
    LocalizationContext rootLocalizationContext = getLocalizationContext(Locale.getDefault());

    // Configure AWS settings
    // Although these objects are configured at the launcher level,
    // we want their localization contexts to be nested inside the cloud provider context.
    LocalizationContext cloudLocalizationContext =
        AWSProvider.METADATA.getLocalizationContext(rootLocalizationContext);
    ephemeralDeviceMappings = getEphemeralDeviceMappings(config, configurationDirectory,
        cloudLocalizationContext);
    virtualizationMappings = getVirtualizationMappings(config, configurationDirectory,
        cloudLocalizationContext);
    rdsEndpoints = getRDSEndpoints(config, configurationDirectory, cloudLocalizationContext);
    awsClientConfig = getAWSClientConfig(config,
        (httpProxyParameters == null) ? new HttpProxyParameters() : httpProxyParameters,
        cloudLocalizationContext);
    awsFilters = getAWSFilterConfig(config);
  }

  @Override
  public CloudProvider createCloudProvider(String cloudProviderId, Configured configuration,
      Locale locale) {
    if (!AWSProvider.METADATA.getId().equals(cloudProviderId)) {
      throw new IllegalArgumentException("Invalid cloud provider: " + cloudProviderId);
    }
    return new AWSProvider(configuration, ephemeralDeviceMappings, virtualizationMappings,
        rdsEndpoints, awsClientConfig, awsFilters, getLocalizationContext(locale));
  }

  /**
   * Parses the specified configuration file.
   *
   * @param configFile the configuration file
   * @return the parsed configuration
   */
  private static Config parseConfigFile(File configFile) {
    ConfigParseOptions options = ConfigParseOptions.defaults()
        .setSyntax(ConfigSyntax.CONF)
        .setAllowMissing(false);

    return ConfigFactory.parseFileAnySyntax(configFile, options);
  }
}