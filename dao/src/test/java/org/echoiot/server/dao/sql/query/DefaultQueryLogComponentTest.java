package org.echoiot.server.dao.sql.query;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.TenantId;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;

@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DefaultQueryLogComponent.class)
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "sql.log_queries=true",
        "sql.log_queries_threshold:2999"
})

public class DefaultQueryLogComponentTest {

    private TenantId tenantId;
    private QueryContext ctx;

    @SpyBean
    private DefaultQueryLogComponent queryLog;

    @Before
    public void setUp() {
        tenantId = new TenantId(UUID.fromString("97275c1c-9cf2-4d25-a68d-933031158f84"));
        ctx = new QueryContext(new QuerySecurityContext(tenantId, null, EntityType.ALARM));
    }

    @Test
    public void logQuery() {

        BDDMockito.willReturn("").given(queryLog).substituteParametersInSqlString("", ctx);
        queryLog.logQuery(ctx, "", 3000);

        Mockito.verify(queryLog, times(1)).substituteParametersInSqlString("", ctx);

    }

    @Test
    public void substituteParametersInSqlString_StringType() {

        String sql = "Select * from Table Where name = :name AND id = :id";
        String sqlToUse = "Select * from Table Where name = 'Mery''s' AND id = 'ID_1'";

        ctx.addStringParameter("name", "Mery's");
        ctx.addStringParameter("id", "ID_1");

        String sqlToUseResult = queryLog.substituteParametersInSqlString(sql, ctx);
        assertEquals(sqlToUse, sqlToUseResult);
    }

    @Test
    public void substituteParametersInSqlString_DoubleLongType() {

        double sum = 0.00000021d;
        long price = 100000;
        String sql = "Select * from Table Where sum = :sum AND price = :price";
        String sqlToUse = "Select * from Table Where sum = 2.1E-7 AND price = 100000";

        ctx.addDoubleParameter("sum", sum);
        ctx.addLongParameter("price", price);

        String sqlToUseResult = queryLog.substituteParametersInSqlString(sql, ctx);
        assertEquals(sqlToUse, sqlToUseResult);
    }

    @Test
    public void substituteParametersInSqlString_BooleanType() {

        String sql = "Select * from Table Where check = :check AND mark = :mark";
        String sqlToUse = "Select * from Table Where check = true AND mark = false";

        ctx.addBooleanParameter("check", true);
        ctx.addBooleanParameter("mark", false);

        String sqlToUseResult = queryLog.substituteParametersInSqlString(sql, ctx);
        assertEquals(sqlToUse, sqlToUseResult);
    }

    @Test
    public void substituteParametersInSqlString_UuidType() {

        UUID guid = UUID.randomUUID();
        String sql = "Select * from Table Where guid = :guid";
        String sqlToUse = "Select * from Table Where guid = '" + guid + "'";

        ctx.addUuidParameter("guid", guid);

        String sqlToUseResult = queryLog.substituteParametersInSqlString(sql, ctx);
        assertEquals(sqlToUse, sqlToUseResult);
    }

    @Test
    public void substituteParametersInSqlString_StringListType() {

        List<String> ids = List.of("ID_1'", "ID_2", "ID_3", "ID_4");

        String sql = "Select * from Table Where id IN (:ids)";
        String sqlToUse = "Select * from Table Where id IN ('ID_1''', 'ID_2', 'ID_3', 'ID_4')";

        ctx.addStringListParameter("ids", ids);

        String sqlToUseResult = queryLog.substituteParametersInSqlString(sql, ctx);
        assertEquals(sqlToUse, sqlToUseResult);
    }

    @Test
    public void substituteParametersInSqlString_UuidListType() {

        List<UUID> guids = List.of(UUID.fromString("634a8d03-6871-4e01-94d0-876bf3e67dff"), UUID.fromString("3adbb5b8-4dc6-4faf-80dc-681a7b518b5e"), UUID.fromString("63a50f0c-2058-4d1d-8f15-812eb7f84412"));

        String sql = "Select * from Table Where guid IN (:guids)";
        String sqlToUse = "Select * from Table Where guid IN ('634a8d03-6871-4e01-94d0-876bf3e67dff', '3adbb5b8-4dc6-4faf-80dc-681a7b518b5e', '63a50f0c-2058-4d1d-8f15-812eb7f84412')";

        ctx.addUuidListParameter("guids", guids);

        String sqlToUseResult = queryLog.substituteParametersInSqlString(sql, ctx);
        assertEquals(sqlToUse, sqlToUseResult);
    }
}
