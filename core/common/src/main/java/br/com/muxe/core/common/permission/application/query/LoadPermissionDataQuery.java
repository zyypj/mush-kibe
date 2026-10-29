package br.com.muxe.core.common.permission.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.permission.domain.PermissionData;

public record LoadPermissionDataQuery() implements Query<PermissionData> {
}
