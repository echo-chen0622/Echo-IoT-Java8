package org.echoiot.server.service.install;

public interface DatabaseSchemaService {

    void createDatabaseSchema() throws Exception;

    void createDatabaseSchema(boolean createIndexes) throws Exception;

    void createDatabaseIndexes() throws Exception;

}
