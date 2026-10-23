package br.com.muxe.core.common.messaging.idempotency;

import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class IdempotencyGuard {

    private final String receiverId;
    private final IdempotencyStore local;
    private final IdempotencyStore distributed;
    private final Logger logger;

    public IdempotencyGuard(String receiverId, IdempotencyStore local, IdempotencyStore distributed, Logger logger) {
        this.receiverId = receiverId;
        this.local = local;
        this.distributed = distributed;
        this.logger = logger;
    }

    public ClaimResult claim(String typeId, String key, DeliveryPolicy policy) {
        String scopedKey = typeId + ":" + key;
        return switch (policy.idempotency()) {
            case NONE -> ClaimResult.FIRST;
            case LOCAL -> claimLocally(scopedKey, policy);
            case DURABLE -> claimDurably(scopedKey, policy);
            case EXCLUSIVE -> claimExclusively(scopedKey, policy);
        };
    }

    private ClaimResult claimLocally(String key, DeliveryPolicy policy) {
        return local.tryClaim(key, policy.deduplicationWindow()) ? ClaimResult.FIRST : ClaimResult.DUPLICATE;
    }

    private ClaimResult claimDurably(String key, DeliveryPolicy policy) {
        if (claimLocally(key, policy) == ClaimResult.DUPLICATE) {
            return ClaimResult.DUPLICATE;
        }
        try {
            return distributed.tryClaim(receiverId + ":" + key, policy.deduplicationWindow()) ? ClaimResult.FIRST : ClaimResult.DUPLICATE;
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Durable idempotency check unavailable, relying on local memory for " + key + ": " + exception.getMessage());
            return ClaimResult.FIRST;
        }
    }

    private ClaimResult claimExclusively(String key, DeliveryPolicy policy) {
        if (claimLocally(key, policy) == ClaimResult.DUPLICATE) {
            return ClaimResult.DUPLICATE;
        }
        try {
            return distributed.tryClaim("network:" + key, policy.deduplicationWindow()) ? ClaimResult.FIRST : ClaimResult.DUPLICATE;
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Exclusive claim unavailable, dropping " + key + ": " + exception.getMessage());
            return ClaimResult.UNAVAILABLE;
        }
    }
}
