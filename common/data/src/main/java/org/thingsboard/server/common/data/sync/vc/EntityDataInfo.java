package org.thingsboard.server.common.data.sync.vc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntityDataInfo {
    boolean hasRelations;
    boolean hasAttributes;
    boolean hasCredentials;
}
