## What is the j2e-pac4j library? [![Build Status](https://travis-ci.org/pac4j/j2e-pac4j.png?branch=master)](https://travis-ci.org/pac4j/j2e-pac4j)

The `j2e-pac4j` project is an authentication / authorization security library for J2E. It's available under the Apache 2 license and based on the [pac4j](https://github.com/pac4j/pac4j) library.

It supports stateful / indirect and stateless / direct [authentication flows](https://github.com/pac4j/pac4j/wiki/Authentication-flows) using external identity providers or internal credentials authenticators and user profile creators:

1. **OAuth** (1.0 & 2.0): Facebook, Twitter, Google, Yahoo, LinkedIn, Github... using the `pac4j-oauth` module
2. **CAS** (1.0, 2.0, SAML, logout & proxy) + REST API support using the `pac4j-cas` module
3. **HTTP** (form, basic auth, IP, header, GET/POST parameter authentications) using the `pac4j-http` module
4. **OpenID** using the `pac4j-openid` module
5. **SAML** (2.0) using the `pac4j-saml` module
6. **Google App Engine** UserService using the `pac4j-gae` module
7. **OpenID Connect** 1.0 using the `pac4j-oidc` module
8. **JWT** using the `pac4j-jwt` module
9. **LDAP** using the `pac4j-ldap` module
10. **relational DB** using the `pac4j-sql` module
11. **MongoDB** using the `pac4j-mongo` module
12. [**Stormpath**](https://stormpath.com) using the `pac4j-stormpath` module.

See [all authentication mechanisms](https://github.com/pac4j/pac4j/wiki/Clients)


## Technical description

This project has **only 4 classes**:

1. the `AbstractConfigFilter` is an abstract J2E filter to manage the configuration
2. the `RequiresAuthenticationFilter` is a J2E filter to protect urls and requires authentication / authorization
3. the `CallbackFilter` is a J2E filter to handle the callback from an identity provider after login to finish the authentication process
4. the `ApplicationLogoutFilter` is a J2E filter to manage the application logout

and is based on the `pac4j-core` library. Learn more by browsing the [j2e-pac4j Javadoc](http://www.pac4j.org/apidocs/j2e-pac4j/index.html) and the [pac4j Javadoc](http://www.pac4j.org/apidocs/pac4j/index.html).


## How to use it?

### Add the required dependencies (`j2e-pac4j` + `pac4j-*` libraries)

You need to add a dependency on the `j2e-pac4j` library (<em>groupId</em>: **org.pac4j**, *latest version*: **1.2.0-SNAPSHOT**) as well as on the appropriate `pac4j` modules (<em>groupId</em>: **org.pac4j**, *version*: **1.8.0-SNAPSHOT**): the `pac4j-oauth` dependency for OAuth support, the `pac4j-cas` dependency for CAS support, the `pac4j-ldap` module for LDAP authentication, ...  

As snapshot dependencies are only available in the [Sonatype snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/org/pac4j/), this repository must be added in the Maven *pom.xml* file for example:

    <repositories>
      <repository>
        <id>sonatype-nexus-snapshots</id>
        <name>Sonatype Nexus Snapshots</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <releases>
          <enabled>false</enabled>
        </releases>
        <snapshots>
          <enabled>true</enabled>
        </snapshots>
      </repository>
    </repositories>

### Define the configuration (`Config` + `Clients` + `XXXClient` + `Authorizer`s)

Each authentication mechanism (Facebook, Twitter, a CAS server...) is defined by a client (implementing the `org.pac4j.core.client.Client` interface). All clients must be gathered in a `org.pac4j.core.client.Clients` class.  
They can be defined in a specific class implementing the `org.pac4j.core.config.ConfigFactory` interface to build a `org.pac4j.core.config.Config` which contains the `Clients` as well as the authorizers which will be used by the application. For example:

    public class DemoConfigFactory implements ConfigFactory {
    
        @Override
        public Config build() {
            final OidcClient oidcClient = new OidcClient();
            oidcClient.setClientID("id");
            oidcClient.setSecret("secret");
            oidcClient.setDiscoveryURI("https://accounts.google.com/.well-known/openid-configuration");
            oidcClient.addCustomParam("prompt", "consent");
    
            final SAML2ClientConfiguration cfg = new SAML2ClientConfiguration("resource:samlKeystore.jks",
                    "pac4j-demo-passwd", "pac4j-demo-passwd", "resource:testshib-providers.xml");
            cfg.setMaximumAuthenticationLifetime(3600);
            cfg.setServiceProviderEntityId("urn:mace:saml:pac4j.org");
            cfg.setServiceProviderMetadataPath(new File("target", "sp-metadata.xml").getAbsolutePath());
            final SAML2Client saml2Client = new SAML2Client(cfg);
    
            final FacebookClient facebookClient = new FacebookClient("fbId", "fbSecret");
            final TwitterClient twitterClient = new TwitterClient("twId", "twSecret");
    
            final FormClient formClient = new FormClient("http://localhost:8080/theForm.jsp", new SimpleTestUsernamePasswordAuthenticator());
            final IndirectBasicAuthClient basicAuthClient = new IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());
    
            final CasClient casClient = new CasClient();
            casClient.setCasLoginUrl("http://mycasserver/login");
    
            ParameterClient parameterClient = new ParameterClient("token", new JwtAuthenticator("salt"));
    
            final Clients clients = new Clients("http://localhost:8080/callback", oidcClient, saml2Client, facebookClient,
                    twitterClient, formClient, basicAuthClient, casClient, parameterClient);
    
            final Config config = new Config(clients);
            config.addAuthorizer("requireRoleAdmin", new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
            config.addAuthorizer("customAuthorizer", new CustomAuthorizer());
    
            return config;
        }
    }

"http://localhost:8080/callback" is the url of the callback endpoint (see below). It may not be defined for REST support only.

If your application is configured via dependency injection, no factory is required to build the configuration. Only the `Clients` object will be defined.


### Define the callback endpoint (only for stateful / indirect authentication mechanisms)

Some authentication mechanisms rely on external identity providers (like Facebook) and thus require to define a callback endpoint where the user will be redirected after login at the identity provider. For REST support only, this callback endpoint is not necessary.  
It must be defined in the *web.xml* file by the `CallbackFilter`:

    <filter>
        <filter-name>callbackFilter</filter-name>
        <filter-class>org.pac4j.j2e.filter.CallbackFilter</filter-class>
        <init-param>
        	<param-name>defaultUrl</param-name>
        	<param-value>/</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>callbackFilter</filter-name>
        <url-pattern>/callback</url-pattern>
        <dispatcher>REQUEST</dispatcher>
    </filter-mapping>

The `defaultUrl` parameter defines where the user will be redirected after login if no url was originally requested.

Using dependency injection via Spring for example, you can define the callback filter as a `DelegatingFilterProxy` in the *web.xml* file:

    <filter>
        <filter-name>callbackFilter</filter-name>
        <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>callbackFilter</filter-name>
        <url-pattern>/callback</url-pattern>
        <dispatcher>REQUEST</dispatcher>
    </filter-mapping>
    
and the specific bean in the *application-context.xml* file:

    <bean id="callbackFilter" class="org.pac4j.j2e.filter.CallbackFilter">
        <property name="defaultUrl" value="/" />
    </bean>


### Protect an url (authentication + authorization)

You can protect an url and require the user to be authenticated by a client (and optionally have the appropriate authorizations = roles / permissions) by using the `RequiresAuthenticationFilter`:

    <filter>
        <filter-name>FacebookAdminFilter</filter-name>
        <filter-class>org.pac4j.j2e.filter.RequiresAuthenticationFilter</filter-class>
        <init-param>
            <param-name>configFactory</param-name>
            <param-value>org.pac4j.demo.j2e.config.DemoConfigFactory</param-value>
        </init-param>
        <init-param>
            <param-name>clientName</param-name>
            <param-value>FacebookClient</param-value>
        </init-param>
        <init-param>
            <param-name>authorizerName</param-name>
            <param-value>requireRoleAdmin</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>FacebookAdminFilter</filter-name>
        <url-pattern>/facebookadmin/*</url-pattern>
        <dispatcher>REQUEST</dispatcher>
    </filter-mapping>

The following parameters can be defined:

- `clientName` (optional): the client name to start or perform the authentication (the first one if it is a list of client names). If the authentication mechanism is specified via the *client_name* request parameter, it must match one of the client names
- `configFactory`: the factory to initialize the configuration: clients and authorizers (only one filter needs to define it as the configuration is shared)
- `authorizerName` (optional): the authorizer name which will protect the resource (it must exist in the authorizers configuration)

This filter can be defined via dependency injection as well. In that case, the `configFactory` and `authorizerName` parameters are not necessary and the `setClients` and `setAuthorizer` methods must be used to define all the clients and the authorizer in charge of the resource protection. 

Define the appropriate `org.pac4j.core.authorization.AuthorizationGenerator` and attach it to the client (using the `addAuthorizationGenerator` method) to compute the roles / permissions of the authenticated user.


### Get redirection urls

You can also explicitly compute a redirection url to a provider by using the `getRedirectAction` method of the client, in order to create an explicit link for login. For example with Facebook:

	Clients client = ConfigSingleton.getConfig().getClients();
	FacebookClient fbClient = (FacebookClient) client.findClient("FacebookClient");
	WebContext context = new J2EContext(request, response);
	String fbLoginUrl = fbClient.getRedirectAction(context, false).getLocation();


### Get the user profile

You can test if the user is authenticated using the `ProfileManager.isAuthenticated()` method or get the user profile using the `ProfileManager.get(true)` method (`false` not to use the session, but only the current HTTP request).

The retrieved profile is at least a `CommonProfile`, from which you can retrieve the most common properties that all profiles share. But you can also cast the user profile to the appropriate profile according to the provider used for authentication. For example, after a Facebook authentication:
 
    FacebookProfile facebookProfile = (FacebookProfile) commonProfile;


### Logout

You can log out the current authenticated user using the `ApplicationLogoutFilter`:

    <filter>
        <filter-name>logoutFilter</filter-name>
        <filter-class>org.pac4j.j2e.filter.ApplicationLogoutFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>logoutFilter</filter-name>
        <url-pattern>/logout</url-pattern>
        <dispatcher>REQUEST</dispatcher>
    </filter-mapping>

and by calling the logout url ("/logout"). A blank page is displayed by default unless an *url* parameter is provided. In that case, the user will be redirected to this specified url (if it matches the logout url pattern defined) or to the default logout url otherwise.

The following parameters can be defined:

- `defaultUrl`: the default logout url if the provided *url* parameter does not match the `logoutUrlPattern`
- `logoutUrlPattern`: the logout url pattern that the logout url must match (it's a security check, only relative urls are allowed by default).


## Migration guide

The `isAjax` parameter is no longer available as AJAX requests are now automatically detected. The `requireAnyRole` and `requieAllRoles` parameters are no longer available and authorizers must be used instead (by name or defined via setter).


## Demo

The demo webapp: [j2e-pac4j-demo](https://github.com/pac4j/j2e-pac4j-demo) is available for tests and implement many authentication mechanisms: Facebook, Twitter, form, basic auth, CAS, SAML, OpenID Connect, JWT...


## Release notes

See the [release notes](https://github.com/pac4j/j2e-pac4j/wiki/Release-Notes).


## Need help?

If you have any question, please use the following mailing lists:

- [pac4j users](https://groups.google.com/forum/?hl=en#!forum/pac4j-users)
- [pac4j developers](https://groups.google.com/forum/?hl=en#!forum/pac4j-dev)
