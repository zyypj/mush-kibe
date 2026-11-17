package br.com.muxe.core.common.permission.domain;

import java.time.Instant;

public interface UserGrants {

    UserGrants NONE = new UserGrants() {

        @Override
        public boolean concerns(PermissionUser after) {
            return false;
        }

        @Override
        public void changed(PermissionUser before, PermissionUser after, Instant now) {
        }
    };

    boolean concerns(PermissionUser after);

    void changed(PermissionUser before, PermissionUser after, Instant now);
}
