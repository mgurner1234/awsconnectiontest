package io.openraven;

import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.plugins.aws.discovery.ClientCreators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.sts.StsClient;

import java.util.Arrays;
import java.util.Optional;

@SpringBootApplication
public class AwsConnectionTestApp implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory
            .getLogger(AwsConnectionTestApp.class);

    public static void main(String[] args) {
        LOG.info("STARTING THE APPLICATION");
        SpringApplication.run(AwsConnectionTestApp.class, args);
        LOG.info("APPLICATION FINISHED");
    }

    @Value("${arrayOfRegions}")
    private String[] arrayOfRegions;

    @Override
    public void run(String... args) {

        Arrays.stream(args).forEach(role -> {
            Arrays.stream(arrayOfRegions).forEach(region -> {
                LOG.debug("Attempting connect for {} -> {}", role, region);

                try {
                    final var clientCreator = ClientCreators.assumeRoleCreator(Region.of(region), role, Optional.empty());
                    LOG.debug("Assume Role Creator created for {} -> {}", role, region);

                    try (final var client = clientCreator.apply(StsClient.builder()).build()) {
                        LOG.debug("clientCreator apply complete for StsClient {} -> {} ", role, region);

                        final String account = client.getCallerIdentity().account();
                        LOG.debug("caller Identity {} -> {} -> {} ", role, region, account);

                        try (final var ec2Client = clientCreator.apply(Ec2Client.builder()).build()) {
                            LOG.debug("clientCreator apply complete for Ec2Client {} -> {} ", role, region);
                        } catch (Exception exception) {
                            LOG.debug("Error on clientCreator.apply:", exception);
                        }

                    } catch (Exception ex) {
                        LOG.debug("Error:", ex);
                    }
                } catch (Exception e) {
                    LOG.debug("Error:", e);

                }
            });
        });
    }
}
