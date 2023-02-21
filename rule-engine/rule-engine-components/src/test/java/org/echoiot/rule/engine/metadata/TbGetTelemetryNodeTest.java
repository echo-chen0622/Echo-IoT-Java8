package org.echoiot.rule.engine.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.server.common.data.kv.Aggregation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class TbGetTelemetryNodeTest {

    TbGetTelemetryNode node;
    TbGetTelemetryNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;

    @Before
    public void setUp() throws Exception {
        ctx = mock(TbContext.class);
        node = spy(new TbGetTelemetryNode());
        config = new TbGetTelemetryNodeConfiguration();
        config.setFetchMode("ALL");
        @NotNull ObjectMapper mapper = JacksonUtil.OBJECT_MAPPER;
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        willCallRealMethod().given(node).parseAggregationConfig(any());
    }

    @Test
    public void givenAggregationAsString_whenParseAggregation_thenReturnEnum() {
        //compatibility with old configs without "aggregation" parameter
        assertThat(node.parseAggregationConfig(null), is(Aggregation.NONE));
        assertThat(node.parseAggregationConfig(""), is(Aggregation.NONE));

        //common values
        assertThat(node.parseAggregationConfig("MIN"), is(Aggregation.MIN));
        assertThat(node.parseAggregationConfig("MAX"), is(Aggregation.MAX));
        assertThat(node.parseAggregationConfig("AVG"), is(Aggregation.AVG));
        assertThat(node.parseAggregationConfig("SUM"), is(Aggregation.SUM));
        assertThat(node.parseAggregationConfig("COUNT"), is(Aggregation.COUNT));
        assertThat(node.parseAggregationConfig("NONE"), is(Aggregation.NONE));

        //all possible values in future
        for (@NotNull Aggregation aggEnum : Aggregation.values()) {
            assertThat(node.parseAggregationConfig(aggEnum.name()), is(aggEnum));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenAggregationWhiteSpace_whenParseAggregation_thenException() {
        node.parseAggregationConfig(" ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenAggregationIncorrect_whenParseAggregation_thenException() {
        node.parseAggregationConfig("TOP");
    }

}
