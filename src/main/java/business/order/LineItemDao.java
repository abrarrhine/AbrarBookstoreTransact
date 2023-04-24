package business.order;

import java.sql.Connection;
import java.util.List;

/**
 * Created by Abrar on 04/07/2023.
 */
public interface LineItemDao {

    public void create(Connection connection, long orderId, long bookId, int quantity);

    public List<LineItem> findByOrderId(long orderId);
}
