package iudx.aaa.server.policy;

import com.hazelcast.spi.impl.eventservice.impl.Registration;
import io.vertx.core.json.JsonObject;
import iudx.aaa.server.registration.RegistrationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import iudx.aaa.server.postgres.client.PostgresClient;

import static iudx.aaa.server.policy.Constants.REGISTRATION_SERVICE_ADDRESS;
import static iudx.aaa.server.token.Constants.POLICY_SERVICE_ADDRESS;

/**
 * The Policy Verticle.
 * <h1>Policy Verticle</h1>
 * <p>
 * The Policy Verticle implementation in the the IUDX AAA Server exposes the
 * {@link iudx.aaa.server.policy.PolicyService} over the Vert.x Event Bus.
 * </p>
 *
 * @version 1.0
 * @since 2020-12-15
 */

public class PolicyVerticle extends AbstractVerticle {

  /* Database Properties */
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;
  private PgPool pgclient;
  private PoolOptions poolOptions;
  private PgConnectOptions connectOptions;
  private JsonObject catalogueOptions;
  private JsonObject authOptions;
  private JsonObject catOptions;
  private PostgresClient pgClient;
  private static final String POLICY_SERVICE_ADDRESS = "iudx.aaa.policy.service";
  private PolicyService policyService;
  private RegistrationService registrationService;
  private CatalogueClient catalogueClient;
  private static final Logger LOGGER = LogManager.getLogger(PolicyVerticle.class);

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   */

  @Override
  public void start() throws Exception {

    /* Read the configuration and set the postgres client properties. */
    LOGGER.debug("Info : " + LOGGER.getName() + " : Reading config file");

    databaseIP = config().getString("databaseIP");
    databasePort = Integer.parseInt(config().getString("databasePort"));
    databaseName = config().getString("databaseName");
    databaseUserName = config().getString("databaseUserName");
    databasePassword = config().getString("databasePassword");
    poolSize = Integer.parseInt(config().getString("poolSize"));
    catalogueOptions = config().getJsonObject("catalogueOptions");
    authOptions = config().getJsonObject("authOptions");
    catOptions = config().getJsonObject("catOptions");


    //get options for catalogue client

    /* Set Connection Object */
    if (connectOptions == null) {
      connectOptions = new PgConnectOptions().setPort(databasePort).setHost(databaseIP)
          .setDatabase(databaseName).setUser(databaseUserName).setPassword(databasePassword);
    }

    /* Pool options */
    if (poolOptions == null) {
      poolOptions = new PoolOptions().setMaxSize(poolSize);
    }

    /* Create the client pool */
   // pgclient = PgPool.pool(vertx, connectOptions, poolOptions);

    //pgClient = new PostgresClient(vertx, connectOptions, poolOptions);

    PgPool pool = PgPool.pool(vertx,connectOptions, poolOptions);
    registrationService = RegistrationService.createProxy(vertx, REGISTRATION_SERVICE_ADDRESS);
    catalogueClient = new CatalogueClient(vertx,pool,catalogueOptions);
    policyService = new PolicyServiceImpl(pool,registrationService,catalogueClient,authOptions,catOptions);

    new ServiceBinder(vertx).setAddress(POLICY_SERVICE_ADDRESS).register(PolicyService.class,
        policyService);

    LOGGER.debug("Info : " + LOGGER.getName() + " : Started");

  }

}
