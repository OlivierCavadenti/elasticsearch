/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.action.service;

import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.security.action.service.GetServiceAccountRequest;
import org.elasticsearch.xpack.core.security.action.service.GetServiceAccountResponse;
import org.elasticsearch.xpack.core.security.action.service.ServiceAccountInfo;
import org.elasticsearch.xpack.security.authc.support.HttpTlsRuntimeCheck;
import org.junit.Before;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class TransportGetServiceAccountActionTests extends ESTestCase {

    private HttpTlsRuntimeCheck httpTlsRuntimeCheck;
    private TransportGetServiceAccountAction transportGetServiceAccountAction;

    @Before
    public void init() {
        httpTlsRuntimeCheck = mock(HttpTlsRuntimeCheck.class);
        transportGetServiceAccountAction = new TransportGetServiceAccountAction(
            mock(TransportService.class), new ActionFilters(Collections.emptySet()), httpTlsRuntimeCheck);

        doAnswer(invocationOnMock -> {
            final Object[] arguments = invocationOnMock.getArguments();
            ((Runnable) arguments[2]).run();
            return null;
        }).when(httpTlsRuntimeCheck).checkTlsThenExecute(any(), any(), any());
    }

    public void testDoExecute() {
        final GetServiceAccountRequest request1 = randomFrom(new GetServiceAccountRequest(null, null),
            new GetServiceAccountRequest("elastic", null));
        final PlainActionFuture<GetServiceAccountResponse> future1 = new PlainActionFuture<>();
        transportGetServiceAccountAction.doExecute(mock(Task.class), request1, future1);
        final GetServiceAccountResponse getServiceAccountResponse1 = future1.actionGet();
        assertThat(getServiceAccountResponse1.getServiceAccountInfos().length, equalTo(2));
        assertThat(
            Arrays.stream(getServiceAccountResponse1.getServiceAccountInfos())
                .map(ServiceAccountInfo::getPrincipal)
                .collect(Collectors.toList()),
            containsInAnyOrder("elastic/fleet-server", "elastic/kibana")
        );

        final GetServiceAccountRequest request2 = new GetServiceAccountRequest("elastic", "fleet-server");
        final PlainActionFuture<GetServiceAccountResponse> future2 = new PlainActionFuture<>();
        transportGetServiceAccountAction.doExecute(mock(Task.class), request2, future2);
        final GetServiceAccountResponse getServiceAccountResponse2 = future2.actionGet();
        assertThat(getServiceAccountResponse2.getServiceAccountInfos().length, equalTo(1));
        assertThat(getServiceAccountResponse2.getServiceAccountInfos()[0].getPrincipal(), equalTo("elastic/fleet-server"));

        final GetServiceAccountRequest request3 = randomFrom(
            new GetServiceAccountRequest("foo", null),
            new GetServiceAccountRequest("elastic", "foo"),
            new GetServiceAccountRequest("foo", "bar"));
        final PlainActionFuture<GetServiceAccountResponse> future3 = new PlainActionFuture<>();
        transportGetServiceAccountAction.doExecute(mock(Task.class), request3, future3);
        final GetServiceAccountResponse getServiceAccountResponse3 = future3.actionGet();
        assertThat(getServiceAccountResponse3.getServiceAccountInfos().length, equalTo(0));
    }
}
