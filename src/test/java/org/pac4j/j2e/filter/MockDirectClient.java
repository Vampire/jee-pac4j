package org.pac4j.j2e.filter;

import org.pac4j.core.client.DirectClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.CommonProfile;

/**
 * Mock an indirect client.
 *
 * @author Jerome Leleu
 * @since 1.3.0
 */
public final class MockDirectClient extends DirectClient<Credentials, CommonProfile> {

    private final ReturnCredentials returnCredentials;

    private final CommonProfile profile;

    public MockDirectClient(final String name, final Credentials credentials, final CommonProfile profile) {
        this(name, () -> credentials, profile);
    }

    public MockDirectClient(final String name, final ReturnCredentials returnCredentials, final CommonProfile profile) {
        setName(name);
        this.returnCredentials = returnCredentials;
        this.profile = profile;
    }

    @Override
    protected void internalInit(final WebContext context) {}

    @Override
    public Credentials getCredentials(WebContext context) throws RequiresHttpAction {
        return returnCredentials.get();
    }

    @Override
    protected CommonProfile retrieveUserProfile(final Credentials credentials, final WebContext context) throws RequiresHttpAction {
        return profile;
    }
}
