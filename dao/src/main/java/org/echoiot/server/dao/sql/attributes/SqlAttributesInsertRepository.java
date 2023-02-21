package org.echoiot.server.dao.sql.attributes;

import org.echoiot.server.dao.util.SqlDao;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
@SqlDao
public class SqlAttributesInsertRepository extends AttributeKvInsertRepository {

}
