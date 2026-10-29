package br.com.muxe.core.common.shop.infrastructure.postgres;

import br.com.muxe.core.common.persistence.PersistenceException;
import br.com.muxe.core.common.persistence.transaction.TransactionContext;
import br.com.muxe.core.common.shop.domain.Promotion;
import br.com.muxe.core.common.shop.domain.PromotionRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

public final class PostgresPromotionRepository implements PromotionRepository {

    private static final String FIND = "SELECT product_id, lines FROM core.shop_promotion WHERE id = 1";

    private static final String SAVE = """
        INSERT INTO core.shop_promotion (id, product_id, lines, changed_at)
        VALUES (1, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE
        SET product_id = EXCLUDED.product_id, lines = EXCLUDED.lines, changed_at = EXCLUDED.changed_at
        """;

    @Override
    public Optional<Promotion> find() {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(FIND); ResultSet rows = statement.executeQuery()) {
            if (!rows.next()) {
                return Optional.empty();
            }
            return Optional.of(new Promotion(rows.getString("product_id"), Arrays.asList((String[]) rows.getArray("lines").getArray())));
        } catch (SQLException exception) {
            throw new PersistenceException("shop.find_promotion", exception);
        }
    }

    @Override
    public void save(Promotion promotion, Instant changedAt) {
        Connection connection = TransactionContext.connection();
        try (PreparedStatement statement = connection.prepareStatement(SAVE)) {
            statement.setString(1, promotion.productId());
            statement.setArray(2, connection.createArrayOf("text", promotion.lines().toArray()));
            statement.setTimestamp(3, Timestamp.from(changedAt));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("shop.save_promotion", exception);
        }
    }
}
