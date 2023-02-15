package org.thingsboard.server.common.data.sync.vc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RepositorySettingsInfo {
    private boolean configured;
    private Boolean readOnly;
}
