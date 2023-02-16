package org.echoiot.server.dao.sql.attributes;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.echoiot.server.dao.util.SqlDao;

@Repository
@Transactional
@SqlDao
public class SqlAttributesInsertRepository extends AttributeKvInsertRepository {

}
