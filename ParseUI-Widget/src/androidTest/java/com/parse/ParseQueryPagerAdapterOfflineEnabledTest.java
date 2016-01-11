package com.parse;

/**
 * Modified by Pablo Baxter (Github: soaboz)
 */
public class ParseQueryPagerAdapterOfflineEnabledTest extends ParseQueryPagerAdapterTest {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        Parse.enableLocalDatastore(null);
    }

    @Override
    public void tearDown() throws Exception {
        Parse.disableLocalDatastore();
        super.tearDown();
    }

    @Override
    public void testLoadObjectsWithCacheThenNetworkQueryAndPagination() throws Exception {
        // Do nothing, there is no cache policy when LDS is enabled.
    }
}
