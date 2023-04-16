
package business;

import business.book.Book;
import business.book.BookDao;
import business.book.BookDaoJdbc;
import business.category.CategoryDao;
import business.category.CategoryDaoJdbc;
import business.order.DefaultOrderService;
import business.order.OrderService;

public class ApplicationContext {
    private CategoryDao categoryDao;
    private BookDao bookDao;

    private OrderService orderService;

    public static ApplicationContext INSTANCE = new ApplicationContext();

    private ApplicationContext() {
        categoryDao = new CategoryDaoJdbc();
        bookDao = new BookDaoJdbc();
        orderService = new DefaultOrderService();
        ((DefaultOrderService)orderService).setBookDao(bookDao);
    }

    public CategoryDao getCategoryDao() {
        return categoryDao;
    }

    public BookDao getBookDao() { return this.bookDao; }
    public OrderService getOrderService(){
        return this.orderService;
    }
}